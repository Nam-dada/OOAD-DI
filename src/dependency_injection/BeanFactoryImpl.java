package dependency_injection;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * TODO you should complete the class
 */
public class BeanFactoryImpl implements BeanFactory {

    private Properties injectPro = new Properties();
    private Properties valuePro = new Properties();
    @Override
    public void loadInjectProperties(File file) {
        try {
            BufferedReader bf = new BufferedReader(new FileReader(file));
            injectPro.load(bf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void loadValueProperties(File file) {
        try {
            BufferedReader bf = new BufferedReader(new FileReader(file));
            valuePro.load(bf);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T createInstance(Class<T> clazz) {
        T ans;
        Class<T> tempClaz = clazz;
        try {
            tempClaz = (Class<T>) Class.forName(injectPro.getProperty(clazz.getName()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Constructor<T> cons = null;
        for (Constructor c : tempClaz.getDeclaredConstructors()){
            if (c.getAnnotation(Inject.class) != null) {
                cons = c;
                break;
            }
        }
        if (cons == null){
            try {
                cons = tempClaz.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        Class<?>[] cons_fieldType = cons.getParameterTypes();
        Parameter[] cons_Parameter = cons.getParameters();
        Object[] para_obj = new Object[cons_fieldType.length];

        Class tempClass;
        Parameter tempPara;
        for (int i = 0; i < cons_fieldType.length; i++) {
            tempClass = cons_fieldType[i];
            tempPara = cons_Parameter[i];

            if (tempPara.getAnnotation(Value.class) == null) {
                para_obj[i] = createInstance(tempClass);
            }else {
                Value valueAnnotation = tempPara.getAnnotation(Value.class);

            }
        }

        return null;
    }

    private <T> Object dealByType(Parameter tempPara, Class<T> type){
        Value valueAnnotation = tempPara.getAnnotation(Value.class);
        String tar = valueAnnotation.value();
        if (valuePro.containsKey(tar)) tar = (String) valuePro.get(tar);
        String[] tarList = tar.split(valueAnnotation.delimiter());

        if (int.class == type) {
            for (String s : tarList) {
                if (!isType(s, type)) continue;
                return Integer.parseInt(s);
            }
            return 0;
        }else if (String.class == type) {
            return tarList[0];
        }else if (boolean.class == type) {
            for (String s : tarList){
                if (isType(s, type)) return Boolean.parseBoolean(s);
            }
            return false;
        }else if (boolean[].class == type) {
            List<String> list = new ArrayList<>();
            for (String s : tarList){
                if (isType(s, boolean.class))
                    list.add(s);
            }

            Boolean[] temp = new Boolean[list.size()];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = Boolean.parseBoolean(list.get(i));
            }
            return temp;
        }else if (int[].class == type) {
            List<Integer> list = new ArrayList<>();
            for (String s : tarList) {
                if (!isType(s, int.class)) continue;
                list.add(Integer.valueOf(s));
            }

            int[] temp = new int[list.size()];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = list.get(i);
            }
            return temp;
        }else if (String[].class == type) {
            return tarList;
        }else{
            try {
                if (List.class == type) {
                    Class<?> listType = Class.forName(((ParameterizedType) tempPara.getParameterizedType())
                            .getActualTypeArguments()[0].getTypeName());
                    List<Object> temp = new ArrayList<>();
                    for (String s : tarList){
                        if (isType(s, listType)) {
                            if (listType == boolean.class) {
                                temp.add(Boolean.parseBoolean(s));
                            }else {
                                temp.add(s);
                            }
                        }
                    }
                    return temp;
                }else if (Set.class == type) {
                    Class<?> setType = Class.forName(((ParameterizedType) tempPara.getParameterizedType())
                            .getActualTypeArguments()[0].getTypeName());
                    Set<Object> temp = new HashSet<>();
                    for (String s : tarList){
                        if (isType(s, setType)) {
                            if (setType == boolean.class) {
                                temp.add(Boolean.parseBoolean(s));
                            }else {
                                temp.add(s);
                            }
                        }
                    }
                    return temp;
                }else if (Map.class == type) {
                    Class<?> keyType = Class.forName(((ParameterizedType) tempPara.getParameterizedType())
                            .getActualTypeArguments()[0].getTypeName());
                    Class<?> valueType = Class.forName(((ParameterizedType) tempPara.getParameterizedType())
                            .getActualTypeArguments()[1].getTypeName());
                    Map<Object, Object> temp = new LinkedHashMap<>();

                    boolean keyBol = keyType == boolean.class;
                    boolean valBol = valueType == boolean.class;
                    for (String s : tarList) {
                        String key = s.split(":")[0];
                        String value = s.split(":")[1];
                        if (isType(key, keyType) && isType(value, valueType)) {
                            Object o = (keyBol) ? Boolean.parseBoolean(key) : key;
                            Object o1 = (valBol) ? Boolean.parseBoolean(value) : value;
                            temp.put(o, o1);
                        }
                    }
                    return temp;
                }
            }catch (Exception e){

            }
        }
        return null;
    }

    private <T> boolean isType(String s, Class<T> type) {
        if (int.class == type) {
            if (s.length() > 10 || !s.matches("^\\d+$")) return false;
            Long i = Long.parseLong(s);
            if (Integer.MIN_VALUE <= i && i <= Integer.MAX_VALUE) return true;
        }else if (String.class == type) {
            return true;
        }else if (boolean.class == type) {
            return s.matches("^[trueTRUE]{4}$") || s.matches("^[falseFALSE]{5}$");
        }
        return false;
    }
}
