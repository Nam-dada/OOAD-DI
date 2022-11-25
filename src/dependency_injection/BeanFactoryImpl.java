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
        T ans = null;
        Class<T> tempClaz = clazz;
        try {
            tempClaz = (Class<T>) Class.forName(injectPro.getProperty(tempClaz.getName()));
        } catch (Exception e) {
        }

        Constructor<T> cons = null;
        for (Constructor c : tempClaz.getDeclaredConstructors()) {
            if (c.getAnnotation(Inject.class) != null) {
                cons = c;
                break;
            }
        }
        if (cons == null) {
            try {
                cons = tempClaz.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        Class<?>[] cons_fieldType = cons.getParameterTypes();
        Parameter[] cons_Parameter = cons.getParameters();
        Object[] para_obj = new Object[cons_fieldType.length];

        Class<?> tempClass;
        Parameter tempPara;
        for (int i = 0; i < cons_fieldType.length; i++) {
            tempClass = cons_fieldType[i];
            tempPara = cons_Parameter[i];

            if (tempPara.getAnnotation(Value.class) == null) {
                para_obj[i] = createInstance(tempClass);
            } else {
                Value valueAnnotation = tempPara.getAnnotation(Value.class);
                if (tempClass == List.class || tempClass == Set.class || tempClass == Map.class) {
                    ParameterizedType pt = (ParameterizedType) tempPara.getParameterizedType();
                    para_obj[i] = dealByType(valueAnnotation, tempClass, pt);
                } else {
                    para_obj[i] = dealByType(valueAnnotation, tempClass);
                }
            }
        }

        try {
            ans = cons.newInstance(para_obj);
            Field[] typeFields = ans.getClass().getDeclaredFields();
            for (Field tf : typeFields) {
                if (tf.getAnnotation(Inject.class) != null) {
                    tf.setAccessible(true);
                    tf.set(ans, createInstance(tf.getType()));
                    tf.setAccessible(false);
                }

                if (tf.getAnnotation(Value.class) != null) {
                    Value valueAnnotation = tf.getAnnotation(Value.class);
                    tf.setAccessible(true);
                    Class<?> fieldClass = tf.getType();
                    if (fieldClass == List.class || fieldClass == Set.class || fieldClass == Map.class) {
                        ParameterizedType pt = (ParameterizedType) tf.getGenericType();
                        tf.set(ans, dealByType(valueAnnotation, fieldClass, pt));
                    }else {
                        tf.set(ans, dealByType(valueAnnotation, fieldClass));
                    }
                    tf.setAccessible(false);
                }
            }

            return ans;
        } catch (Exception e) {
            System.out.println(e);
        }


        return null;
    }

    private <T> Object dealByType(Value valueAnnotation, Class<T> type) {
        String tar = valueAnnotation.value();
        if (valuePro.containsKey(tar)) tar = (String) valuePro.get(tar);
        tar = tar.replace("[", "");
        tar = tar.replace("]", "");
        tar = tar.replace("{", "");
        tar = tar.replace("}", "");
        String[] tarList = tar.split(valueAnnotation.delimiter());

        if (int.class == type) {
            for (String s : tarList) {
                if (!isType(s, type)) continue;
                return Integer.parseInt(s);
            }
            return 0;
        } else if (String.class == type) {
            return tarList[0];
        } else if (boolean.class == type) {
            for (String s : tarList) {
                if (isType(s, type)) return Boolean.parseBoolean(s);
            }
            return false;
        } else if (boolean[].class == type) {
            List<String> list = new ArrayList<>();
            for (String s : tarList) {
                if (isType(s, boolean.class))
                    list.add(s);
            }

            boolean[] temp = new boolean[list.size()];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = Boolean.parseBoolean(list.get(i));
            }
            return temp;
        } else if (int[].class == type) {
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
        } else if (String[].class == type) {
            return tarList;
        }

        return null;
    }

    private <T> Object dealByType(Value valueAnnotation, Class<T> type, ParameterizedType pt) {
        String tar = valueAnnotation.value();
        if (valuePro.containsKey(tar)) tar = (String) valuePro.get(tar);
        tar = tar.replace("[", "");
        tar = tar.replace("]", "");
        tar = tar.replace("{", "");
        tar = tar.replace("}", "");
        String[] tarList = tar.split(valueAnnotation.delimiter());

        try {
            if (List.class == type) {
                Class<?> listType = Class.forName(pt.getActualTypeArguments()[0].getTypeName());
                List<Object> temp = new ArrayList<>();
                for (String s : tarList) {
                    if (isType(s, listType)) temp.add(parseType(s, listType));
                }
                return temp;
            } else if (Set.class == type) {
                Class<?> setType = Class.forName(pt.getActualTypeArguments()[0].getTypeName());
                Set<Object> temp = new HashSet<>();
                for (String s : tarList) {
                    if (isType(s, setType)) temp.add(parseType(s, setType));
                }
                return temp;
            } else if (Map.class == type) {
                Class<?> keyType = Class.forName(pt.getActualTypeArguments()[0].getTypeName());
                Class<?> valueType = Class.forName(pt.getActualTypeArguments()[1].getTypeName());
                Map<Object, Object> temp = new LinkedHashMap<>();

                for (String s : tarList) {
                    String key = s.split(":")[0];
                    String value = s.split(":")[1];
                    if (isType(key, keyType) && isType(value, valueType))
                        temp.put(parseType(key, keyType), parseType(value, valueType));
                }
                return temp;
            }
        } catch (Exception e) {

        }
        return null;
    }

    private <T> Object parseType(String s, Class<T> type) {
        if (type == boolean.class) return Boolean.parseBoolean(s);
        else if (type == Integer.class) return Integer.valueOf(s);
        else return s;
    }

    private <T> boolean isType(String s, Class<T> type) {
        if (int.class == type || Integer.class == type) {
            boolean isNegative = s.charAt(0) == '-';
            if (isNegative) s = s.substring(1);
            if (s.length() > 10 || !s.matches("^\\d+$")) return false;
            long i = Long.parseLong(s);
            if (isNegative) i = -i;
            if (Integer.MIN_VALUE <= i && i <= Integer.MAX_VALUE) return true;
        } else if (String.class == type) {
            return true;
        } else if (boolean.class == type || Boolean.class == type) {
            return s.matches("^[trueTRUE]{4}$") || s.matches("^[falseFALSE]{5}$");
        }
        return false;
    }
}
