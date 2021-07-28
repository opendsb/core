package org.opendsb.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ReflectionUtils {

	public static boolean isSubClass(Class<?> clazz, Class<?> superClass) {
		boolean ret = false;
		
		Class<?> parent = clazz;
		
		while (parent != null) {
			if (parent.equals(superClass)) {
				ret = true;
				break;
			}
			parent = parent.getSuperclass();
		}
		
		return ret;
	}
	
	
	public static Collection<Object> createCollection(Class<?> concreteClass) {
		
		if (!doesImplement(concreteClass, Collection.class)) {
			return null;
		}
		
		if (doesImplement(concreteClass, List.class)) {
			return new ArrayList<>();
		}
		
		if (doesImplement(concreteClass, Set.class)) {
			return new HashSet<>();
		}
		
		if (doesImplement(concreteClass, Queue.class)) {
			return new LinkedList<>();
		}
		
		return null;
	}
	
	
	public static boolean doesImplement(Class<?> clazz, Class<?> targetInterface) {
		boolean ret = false;
		
		Class<?> parent = clazz;
		
		while (parent != null) {
			ret = Arrays.stream(parent.getInterfaces()).map(itf -> {
				if (itf.equals(targetInterface)) {
					return true;
				}
				return false;
			}).reduce(false, (x, y) -> x || y);
			
			if (ret) {
				break;
			}
			parent = parent.getSuperclass();
		}
		
		return ret;
	}
	
}
