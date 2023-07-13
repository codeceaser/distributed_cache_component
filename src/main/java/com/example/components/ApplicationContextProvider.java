package com.example.components;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    public static ApplicationContext appContext;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        appContext = applicationContext;
    }

    public static Object getBean(Class beanClass) {
        return appContext.getBean(beanClass);
    }

    public static Object getBeanUsingQualifier(Class type, String qualifier) {
        return BeanFactoryAnnotationUtils.qualifiedBeansOfType(((AbstractApplicationContext) appContext).getBeanFactory(), type, qualifier).values().stream().findFirst().get();
    }
}
