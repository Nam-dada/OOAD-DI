package dependency_injection;

import java.io.*;
import java.util.Properties;

/**
 * TODO you should complete the class
 */
public class BeanFactoryImpl implements BeanFactory {

    public Properties injectPro = new Properties();
    public Properties valuePro = new Properties();
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
        return null;
    }
}
