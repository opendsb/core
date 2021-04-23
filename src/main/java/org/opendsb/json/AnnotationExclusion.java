package org.opendsb.json;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

public class AnnotationExclusion implements ExclusionStrategy {

	@Override
	public boolean shouldSkipClass(Class<?> arg0) {
		return false;
	}

	@Override
	public boolean shouldSkipField(FieldAttributes f) {
		if (f.getAnnotation(JsonIgnore.class) != null) {
			return true;
		}
		return false;
	}

}
