package com.ytrue.ioc.test;



import com.ytrue.ioc.beans.factory.FactoryBean;

import java.util.HashSet;
import java.util.Set;


public class ConvertersFactoryBean implements FactoryBean<Set<?>> {

	@Override
	public Set<?> getObject() throws Exception {
		HashSet<Object> converters = new HashSet<>();
		StringToLocalDateConverter stringToLocalDateConverter = new StringToLocalDateConverter("yyyy-MM-dd");
		converters.add(stringToLocalDateConverter);
		return converters;
	}

	@Override
	public Class<?> getObjectType() {
		return null;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
