package customer.batchimportcat.batch.exceptions;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

@Component
public class BatchExceptionsUtil {
    // @Autowired
    static ResourceBundleMessageSource messageSource;

    @Autowired
    public void setMessageSource(ResourceBundleMessageSource messageSource) {
        BatchExceptionsUtil.messageSource = messageSource;
    }

    public static BatchConfigNotFound geBatchConfigNotFound(String configName) {
        String message;
        message = messageSource.getMessage("batch.config.notfound", new Object[] { configName },
                Locale.getDefault());

        return new BatchConfigNotFound(message);
    }

    public static BatchFileNotFound getBatchFileNotFound(String UUID) {
        String message;
        message = messageSource.getMessage("batch.record.notfound", new Object[] { UUID },
                Locale.getDefault());

        return new BatchFileNotFound(message);
    }

    public static BatchConfigNotFound getBatchConfigNotFoundBecauseNoStruct(String configName) {
        String message;
        message = messageSource.getMessage("batch.config.structurenotmaintained", new Object[] { configName },
                Locale.getDefault());

        return new BatchConfigNotFound(message);
    }

    public static BatchConfigNotFound getBatchConfigNotFoundBecauseNoStruct(Throwable e) {
        // String message;
        // message = messageSource.getMessage("batch.config.structurenotmaintained", new
        // Object[] { configName },
        // Locale.getDefault());

        return new BatchConfigNotFound(e);
    }

    public static BatchConfigNotFound getBatchConfigNotFoundBecauseNoClass(String configName) {
        String message;
        message = messageSource.getMessage("batch.config.classnotmaintained", new Object[] { configName },
                Locale.getDefault());

        return new BatchConfigNotFound(message);
    }

    public static BatchConfigNotFound getBatchConfigNotFoundBecauseNoClass(Throwable e) {
        // String message;
        // message = messageSource.getMessage("batch.config.classnotmaintained", new
        // Object[] { configName },
        // Locale.getDefault());

        return new BatchConfigNotFound(e);
    }

    public static BatchRecordNotSucess getBatchRecordNotSucess(String CdsName, Integer line) {
        String message;
        message = messageSource.getMessage("batch.record.writenotsuccess", new Object[] { CdsName, line },
                Locale.getDefault());

        return new BatchRecordNotSucess(message);
    }

}
