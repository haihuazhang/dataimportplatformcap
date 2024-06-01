package customer.batchimportcat.batch.classifiers;

import java.util.Map;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.classify.Classifier;

import com.sap.cds.reflect.CdsModel;

import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;
import customer.batchimportcat.batch.itemWriters.SimpleItemWriter;

public class CdsBatchImportClassifier<T> implements Classifier<T, ItemWriter<? super T>> {

    private DefaultListableBeanFactory defaultListableBeanFactory;

    private CdsModel cdsModel;

    private Map<String, ? extends Object> configData;

    public CdsBatchImportClassifier(DefaultListableBeanFactory defaultListableBeanFactory, CdsModel cdsModel,
            Map<String, ? extends Object> configData) {
        this.defaultListableBeanFactory = defaultListableBeanFactory;
        this.cdsModel = cdsModel;
        this.configData = configData;
    }

    @Override
    public ItemWriter<? super T> classify(T classifiable) {

        if (!configData.containsKey("ImplementedByClass")) {
            throw BatchExceptionsUtil.getBatchConfigNotFoundBecauseNoClass(configData.get("ID").toString());
        }
        // get class name from config data
        String writerClassName = configData.get("ImplementedByClass").toString();
        try {

            // get the writer class
            Class<?> writerClass;
            writerClass = Class.forName(writerClassName);

            try {
                Object existWriter = defaultListableBeanFactory.getBean(writerClassName, writerClass);
                return (ItemWriter<T>) existWriter;
            } catch (NoSuchBeanDefinitionException e) {

                // instance the writer
                Object writer = writerClass.getConstructor().newInstance();

                // create spring bean
                defaultListableBeanFactory.initializeBean(writer, writerClassName);
                defaultListableBeanFactory.autowireBeanProperties(writer, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE,
                        true);
                defaultListableBeanFactory.registerSingleton(writerClassName, writer);

                return (ItemWriter<T>) writer;
            }

            // if (existWriter != null) {

            // } else {

            // }
            // return new SimpleItemWriter<T>();

        } catch (Exception e) {
            // TODO: handle exception
            // return null;
            throw BatchExceptionsUtil.getBatchConfigNotFoundBecauseNoClass(e);
        }

    }

}
