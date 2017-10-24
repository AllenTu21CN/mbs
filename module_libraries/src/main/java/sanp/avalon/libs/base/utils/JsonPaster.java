/*
 *  文件名称 : JsonPaster.java <br>
 *  项目名称 : XUNGE-middleware-core<br>
 *  包名称      : com.xunge.middle.util<br>
 *  建立人员 : WuJianPing<br>
 *  建立时间 : 下午2:39:34
 *  Copyright (c) QunLiGroup
 */
package sanp.avalon.libs.base.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * JSON对象设置对象属性
 * @author WuJianPing
 *
 */
public class JsonPaster<T> {
	

	private Class<T> clazz;
	
	private Map<String,Method> mapMethod ;
	private Map<String,Class<?>> mapFieldClass;
	
	public JsonPaster(Class<T> clazz){
		this.clazz = clazz ;
		this.mapMethod = new HashMap<String, Method>();
		this.mapFieldClass=new HashMap<String,Class<?>>();
	}
	
	public T parseProperties(JSONObject json,Map<String,Class<?>> subClazz) throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, JSONException{
		
		T result=(T)this.clazz.newInstance();
		
		Method[] methods = this.clazz.getMethods();
		for(Method method : methods) {
			final String name=method.getName();
			final Class<?>[] types = method.getParameterTypes();
			if(types==null) continue;
			if(types.length==1&&name.length()>3&&name.startsWith("set")&&Character.isUpperCase(name.charAt(3))){
				final StringBuilder sb=new StringBuilder(name.substring(3,4).toLowerCase())
					.append(name.substring(4));
				final String key=sb.toString();
				this.mapMethod.put(key, method);
				this.mapFieldClass.put(key, types[0]);
			}
		}
		Iterator<?> it = json.keys();
		List<String> checked=new ArrayList<String>();
		while(it.hasNext()){
			String key=(String)it.next();
			Object val = json.get(key);
			//if(val instanceof JSO  JSONNull) val = null;

			if(checked.contains(key.toLowerCase())){
				//logger.error("dual Property \""+key+"\" of "+this.clazz.getName());
				continue;
			}
			checked.add(key.toLowerCase());
			
			Method method=mapMethod.get(key);
			Class<?> fieldType;
			if(method!=null){
				fieldType=this.mapFieldClass.get(key);
			}else{
				continue;
			}

			if((val instanceof JSONArray)&& (fieldType==List.class||fieldType==ArrayList.class)){
				if(subClazz!=null&&subClazz.containsKey(key)){
					Class<?> itemClazz=subClazz.get(key);
					JsonPaster<?> paster=new JsonPaster(itemClazz); 
					List<?> list=paster.parseList((JSONArray)val);
					method.invoke(result, list);
				}else{
					continue;
				}
			}else if(fieldType.isAssignableFrom(java.util.Collection.class)||fieldType.isAssignableFrom(java.util.Set.class)||fieldType.isAssignableFrom(Map.class)){
				//logger.debug("not supply "+this.clazz.getName()+"."+method.getName()+"("  +fieldType.getName()+")");
				continue;
			}
			
			if(val==null){
				if(fieldType==Boolean.class){
					val=false;
				}else if(fieldType==Integer.class||fieldType==Long.class||fieldType==Short.class||fieldType==Double.class){
					val=0;
				}else{
					continue;
				}
			}
			
			setFieldValue(result,method,fieldType,val);

		}
		
		return result;
		
		
	}
	
	
	public List<T> parseList(JSONArray arr) throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, JSONException{
		
		List<T> list=new ArrayList<T>();
		int size=arr.length();
		for(int i=0;i<size;i++){
			JSONObject json = arr.getJSONObject(i);
			T obj=parseProperties(json);
			list.add(obj);
		}
		return list;
	}
	
	public T parseProperties(JSONObject json) throws InstantiationException, IllegalAccessException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException, JSONException{
		
		T result=(T)this.clazz.newInstance();
		
		Method[] methods = this.clazz.getMethods();
		for(Method method : methods) {
			final String name=method.getName();
			final Class<?>[] types = method.getParameterTypes();
			if(types==null) continue;
			if(types.length==1&&name.length()>3&&name.startsWith("set")&&Character.isUpperCase(name.charAt(3))){
				final StringBuilder sb=new StringBuilder(name.substring(3,4).toLowerCase())
					.append(name.substring(4));
				final String key=sb.toString();
				this.mapMethod.put(key, method);
				this.mapFieldClass.put(key, types[0]);
			}
		}
		Iterator<?> it = json.keys();
		List<String> checked=new ArrayList<String>();
		while(it.hasNext()){
			String key=(String)it.next();
			Object val = json.get(key);
			//if(val instanceof JSONNull) val = null;

			if(checked.contains(key.toLowerCase())){
				//logger.error("dual Property \""+key+"\" of "+this.clazz.getName());
				continue;
			}
			checked.add(key.toLowerCase());
			
			Method method=mapMethod.get(key);
			Class<?> fieldType;
			if(method!=null){
				fieldType=this.mapFieldClass.get(key);
			}else{
				continue;
			}
			
			if(fieldType.isAssignableFrom(java.util.Collection.class)||fieldType.isAssignableFrom(java.util.Set.class)||fieldType.isAssignableFrom(Map.class)){
				//logger.debug("not supply "+this.clazz.getName()+"."+method.getName()+"("  +fieldType.getName()+")");
				continue;
			}
			
			if(val==null){
				if(fieldType==Boolean.class){
					val=false;
				}else if(fieldType==Integer.class||fieldType==Long.class||fieldType==Short.class||fieldType==Double.class){
					val=0;
				}else{
					continue;
				}
			}
			
			setFieldValue(result,method,fieldType,val);

		}
		
		return result;
	}

	/**
	 * 设置对象的属性
	 * @param instance
	 * @param method
	 * @param fieldType
	 * @param fieldValue
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InstantiationException 
	 * @throws JSONException 
	 */
	private void setFieldValue(T instance, Method method, Class<?> fieldType, Object fieldValue) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, InstantiationException, NoSuchMethodException, SecurityException, JSONException {

//		if(method.getName().equals("setBandType")){
//			System.out.println("==");
//		}
		
		Object value = null ;
		if(fieldType==boolean.class){
			String sval=fieldValue.toString();
			value=isTrue(sval).booleanValue();
		}else if(fieldType==Boolean.class){
			String sval=fieldValue.toString();
			value=isTrue(sval);
		}else if ((fieldType==java.sql.Date.class) && (fieldValue instanceof Timestamp))
        {
        	value = new java.sql.Date(((Timestamp)fieldValue).getTime());
        }else if(fieldValue instanceof Timestamp){
        	if(fieldType==String.class){
        		if(!StringUtils.isEmpty((String)fieldValue)){
	        		SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        		value=fmt.format((Timestamp)fieldValue);
        		}else{
        			return;
        		}
        	}else if(fieldType==java.sql.Date.class){
        		value = new java.sql.Date(((Timestamp)fieldValue).getTime());
        	}else if(fieldType==java.util.Date.class){
        		value = new java.util.Date(((Timestamp)fieldValue).getTime());
        	}else if(fieldType!=Timestamp.class){
        		throw new IllegalArgumentException();
        	}
        }else if (fieldValue instanceof java.math.BigDecimal)     {
        	value=valFromDecimal((java.math.BigDecimal)fieldValue, fieldType);
        } else if (fieldValue instanceof Number) {
        	value=valFromNumber((Number)fieldValue, fieldType);
        } else if(fieldType==String.class){
        	value=fieldValue.toString();
        } else if(fieldType.getName().startsWith("com.xunge") && (fieldValue instanceof JSONObject)){
        	@SuppressWarnings({ "rawtypes", "unchecked" })
			JsonPaster<?> paster=new JsonPaster(fieldType);
        	value = paster.parseProperties((JSONObject)fieldValue);
        }else{
        	//logger.debug("No "+instance.getClass().getName()+"."+method.getName()+"("+fieldType.getName()+") of \""+fieldValue+"\"");
        }
		
        if(value!=null){
        	//System.err.printf("%s <= %s\t%s\n",method.getName(),fieldValue,value);
        	method.invoke(instance, value);
        }
        
	}
	
	private Boolean isTrue(String s){
		return !StringUtils.isEmpty(s)&&(s.equalsIgnoreCase("1")||s.equalsIgnoreCase("true")||s.equals("y")||s.equals("yes") );
	}
	
    private Object valFromDecimal(java.math.BigDecimal bd, Class<?> clz){
    	Object val=bd;
        if (clz.equals(Integer.class)||clz.equals(int.class)) {
            val = bd.intValue();
        } else if (clz.equals(Double.class)||clz.equals(double.class)) {
            val = bd.doubleValue();
       } else if (clz.equals(long.class)||clz.equals(Long.class)) {
            val = bd.longValue();
        } else if (clz.equals(Float.class)||clz.equals(float.class)) {
            val = bd.floatValue();
        } else if (clz.equals(Byte.class)||clz.equals(byte.class)) {
            val = bd.byteValue();
        } else if (clz.equals(Short.class)||clz.equals(short.class)) {
            val = bd.shortValue();
        }else if(clz.equals(String.class)){
        	val=bd.toString();
        }
        return val;
    }
	
    private Object valFromNumber(Number num, Class<?> clz){
    	Object val=num;
        if (clz.equals(Integer.class)) {
        	val = num.intValue();
        } else if (clz.equals(int.class)) {
        	val = num.intValue();
        } else if (clz.equals(Double.class)||clz.equals(double.class)) {
        	val = num.doubleValue();
       } else if (clz.equals(long.class)||clz.equals(Long.class)) {
        	val = num.longValue();
        } else if (clz.equals(Float.class)||clz.equals(float.class)) {
        	val = num.floatValue();
        } else if (clz.equals(Byte.class)) {
        	val = num.byteValue();
        } else if (clz.equals(byte.class)) {
        	val = num.byteValue();
        } else if (clz.equals(short.class)) {
        	val = num.shortValue();
        } else if (clz.equals(Short.class)) {
        	val = num.shortValue();
        } else if(clz.equals(String.class)){
        	val=num.toString();
        }
        return val;
    }
}
