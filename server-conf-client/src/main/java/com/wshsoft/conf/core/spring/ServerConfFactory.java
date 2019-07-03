package com.wshsoft.conf.core.spring;

import com.wshsoft.conf.core.ServerConfClient;
import com.wshsoft.conf.core.annotation.ServerConf;
import com.wshsoft.conf.core.exception.ServerConfException;
import com.wshsoft.conf.core.factory.ServerConfBaseFactory;
import com.wshsoft.conf.core.listener.impl.BeanRefreshServerConfListener;
import com.wshsoft.conf.core.util.FieldReflectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 * ServerConf Factory
 *
 * @author Carry_xie 2015-9-12 19:42:49
 */
public class ServerConfFactory extends InstantiationAwareBeanPostProcessorAdapter
		implements InitializingBean, DisposableBean, BeanNameAware, BeanFactoryAware {

	private static Logger logger = LoggerFactory.getLogger(ServerConfFactory.class);


	// ---------------------- env config ----------------------

	private String envprop;		// like "server-conf.properties" or "file:/data/webapps/server-conf.properties", include the following env config

	private String adminAddress;
	private String env;
	private String accessToken;
	private String mirrorfile;

	public void setAdminAddress(String adminAddress) {
		this.adminAddress = adminAddress;
	}

	public void setEnv(String env) {
		this.env = env;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public void setMirrorfile(String mirrorfile) {
        this.mirrorfile = mirrorfile;
    }

    // ---------------------- init/destroy ----------------------

	@Override
	public void afterPropertiesSet() {
		ServerConfBaseFactory.init(adminAddress, env, accessToken, mirrorfile);
	}

	@Override
	public void destroy() {
		ServerConfBaseFactory.destroy();
	}


	// ---------------------- post process / xml、annotation ----------------------

	@Override
	public boolean postProcessAfterInstantiation(final Object bean, final String beanName) throws BeansException {


		// 1、Annotation('@ServerConf')：resolves conf + watch
		if (!beanName.equals(this.beanName)) {

			ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
				@Override
				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
					if (field.isAnnotationPresent(ServerConf.class)) {
						String propertyName = field.getName();
						ServerConf ServerConf = field.getAnnotation(ServerConf.class);

						String confKey = ServerConf.value();
						String confValue = ServerConfClient.get(confKey, ServerConf.defaultValue());


						// resolves placeholders
						BeanRefreshServerConfListener.BeanField beanField = new BeanRefreshServerConfListener.BeanField(beanName, propertyName);
						refreshBeanField(beanField, confValue, bean);

						// watch
						if (ServerConf.callback()) {
							BeanRefreshServerConfListener.addBeanField(confKey, beanField);
						}

					}
				}
			});
		}

		return super.postProcessAfterInstantiation(bean, beanName);
	}

	@Override
	public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException {

		// 2、XML('$ServerConf{...}')：resolves placeholders + watch
		if (!beanName.equals(this.beanName)) {

			PropertyValue[] pvArray = pvs.getPropertyValues();
			for (PropertyValue pv : pvArray) {
				if (pv.getValue() instanceof TypedStringValue) {
					String propertyName = pv.getName();
					String typeStringVal = ((TypedStringValue) pv.getValue()).getValue();
					if (xmlKeyValid(typeStringVal)) {

						// object + property
						String confKey = xmlKeyParse(typeStringVal);
						String confValue = ServerConfClient.get(confKey, "");

						// resolves placeholders
						BeanRefreshServerConfListener.BeanField beanField = new BeanRefreshServerConfListener.BeanField(beanName, propertyName);
						//refreshBeanField(beanField, confValue, bean);

						Class propClass = String.class;
						for (PropertyDescriptor item: pds) {
							if (beanField.getProperty().equals(item.getName())) {
								propClass = item.getPropertyType();
							}
						}
						Object valueObj = FieldReflectionUtil.parseValue(propClass, confValue);
						pv.setConvertedValue(valueObj);

						// watch
						BeanRefreshServerConfListener.addBeanField(confKey, beanField);

					}
				}
			}

		}

		return super.postProcessPropertyValues(pvs, pds, bean, beanName);
	}

	// ---------------------- refresh bean with server conf  ----------------------

	/**
	 * refresh bean with server conf (fieldNames)
	 */
	public static void refreshBeanField(final BeanRefreshServerConfListener.BeanField beanField, final String value, Object bean){
		if (bean == null) {
			bean = ServerConfFactory.beanFactory.getBean(beanField.getBeanName());		// 已优化：启动时禁止实用，getBean 会导致Bean提前初始化，风险较大；
		}
		if (bean == null) {
			return;
		}

		BeanWrapper beanWrapper = new BeanWrapperImpl(bean);

		// property descriptor
		PropertyDescriptor propertyDescriptor = null;
		PropertyDescriptor[] propertyDescriptors = beanWrapper.getPropertyDescriptors();
		if (propertyDescriptors!=null && propertyDescriptors.length>0) {
			for (PropertyDescriptor item: propertyDescriptors) {
				if (beanField.getProperty().equals(item.getName())) {
					propertyDescriptor = item;
				}
			}
		}

		// refresh field: set or field
		if (propertyDescriptor!=null && propertyDescriptor.getWriteMethod() != null) {
			beanWrapper.setPropertyValue(beanField.getProperty(), value);	// support mult data types
			logger.info(">>>>>>>>>>> server-conf, refreshBeanField[set] success, {}#{}:{}",
					beanField.getBeanName(), beanField.getProperty(), value);
		} else {

			final Object finalBean = bean;
			ReflectionUtils.doWithFields(bean.getClass(), new ReflectionUtils.FieldCallback() {
				@Override
				public void doWith(Field fieldItem) throws IllegalArgumentException, IllegalAccessException {
					if (beanField.getProperty().equals(fieldItem.getName())) {
						try {
							Object valueObj = FieldReflectionUtil.parseValue(fieldItem.getType(), value);

							fieldItem.setAccessible(true);
							fieldItem.set(finalBean, valueObj);		// support mult data types

							logger.info(">>>>>>>>>>> server-conf, refreshBeanField[field] success, {}#{}:{}",
									beanField.getBeanName(), beanField.getProperty(), value);
						} catch (IllegalAccessException e) {
							throw new ServerConfException(e);
						}
					}
				}
			});

			/*Field[] beanFields = bean.getClass().getDeclaredFields();
			if (beanFields!=null && beanFields.length>0) {
				for (Field fieldItem: beanFields) {
					if (beanField.getProperty().equals(fieldItem.getName())) {
						try {
							Object valueObj = FieldReflectionUtil.parseValue(fieldItem.getType(), value);

							fieldItem.setAccessible(true);
							fieldItem.set(bean, valueObj);		// support mult data types

							logger.info(">>>>>>>>>>> server-conf, refreshBeanField[field] success, {}#{}:{}",
									beanField.getBeanName(), beanField.getProperty(), value);
						} catch (IllegalAccessException e) {
							throw new ServerConfException(e);
						}
					}
				}
			}*/
		}

	}


	// ---------------------- util ----------------------

	/**
	 * register beanDefinition If Not Exists
	 *
	 * @param registry
	 * @param beanClass
	 * @param beanName
	 * @return
	 */
	public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, Class<?> beanClass, String beanName) {

		// default bean name
		if (beanName == null) {
			beanName = beanClass.getName();
		}

		if (registry.containsBeanDefinition(beanName)) {	// avoid beanName repeat
			return false;
		}

		String[] beanNameArr = registry.getBeanDefinitionNames();
		for (String beanNameItem : beanNameArr) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanNameItem);
			if (Objects.equals(beanDefinition.getBeanClassName(), beanClass.getName())) {	// avoid className repeat
				return false;
			}
		}

		BeanDefinition annotationProcessor = BeanDefinitionBuilder.genericBeanDefinition(beanClass).getBeanDefinition();
		registry.registerBeanDefinition(beanName, annotationProcessor);
		return true;
	}


	private static final String placeholderPrefix = "$ServerConf{";
	private static final String placeholderSuffix = "}";

	/**
	 * valid xml
	 *
	 * @param originKey
	 * @return
	 */
	private static boolean xmlKeyValid(String originKey){
		boolean start = originKey.startsWith(placeholderPrefix);
		boolean end = originKey.endsWith(placeholderSuffix);
		if (start && end) {
			return true;
		}
		return false;
	}

	/**
	 * parse xml
	 *
	 * @param originKey
	 * @return
	 */
	private static String xmlKeyParse(String originKey){
		if (xmlKeyValid(originKey)) {
			// replace by server-conf
			String key = originKey.substring(placeholderPrefix.length(), originKey.length() - placeholderSuffix.length());
			return key;
		}
		return null;
	}


	// ---------------------- other ----------------------

	private String beanName;
	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}

	private static BeanFactory beanFactory;
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

}
