package customer.batchimportcat.batch.itemReaders.rowMappers;

import org.springframework.batch.extensions.excel.RowMapper;
// import org.springframework.batch.extensions.excel.mapping.BeanWrapperRowMapper;
import org.springframework.batch.extensions.excel.support.rowset.RowSet;
import org.springframework.batch.support.DefaultPropertyEditorRegistrar;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.PropertyAccessorUtils;
// import org.springframework.beans.PropertyMatches;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.sap.cds.Struct;

public class CdsWapperRowMapper<T> extends DefaultPropertyEditorRegistrar implements RowMapper<T> {

    private Class<? extends T> type;
    private final ConcurrentMap<DistanceHolder, ConcurrentMap<String, String>> propertiesMatched = new ConcurrentHashMap();
    private int distanceLimit = 5;
    private boolean strict = true;

    public void setTargetType(Class<? extends T> type) {
        this.type = type;
    }

    @Override
    public T mapRow(RowSet rs) throws BindException {
        // T copy = this.getBean();
        // DataBinder binder = this.createBinder(copy);
        // binder.bind(new MutablePropertyValues(this.getBeanProperties(copy,
        // rs.getProperties())));
        // if (binder.getBindingResult().hasErrors()) {
        // throw new BindException(binder.getBindingResult());
        // } else {
        // return copy;
        // }

        // create by CDS
        T copy = this.createObjectByCDS();
        DataBinder binder = this.createBinder(copy);
        binder.bind(new MutablePropertyValues(this.getBeanProperties(copy, rs.getProperties())));
        if (binder.getBindingResult().hasErrors()) {
            throw new BindException(binder.getBindingResult());
        } else {
            return copy;
        }
    }

    private T createObjectByCDS() {
        return Struct.create(type);
    }

    protected DataBinder createBinder(Object target) {
        DataBinder binder = new DataBinder(target);
        binder.setIgnoreUnknownFields(!this.strict);
        // this.initBinder(binder);
        this.registerCustomEditors(binder);
        return binder;
    }

    private Properties getBeanProperties(Object bean, Properties properties) {
        Class<?> cls = bean.getClass();
        DistanceHolder distanceKey = new DistanceHolder(cls, this.distanceLimit);
        if (!this.propertiesMatched.containsKey(distanceKey)) {
            this.propertiesMatched.putIfAbsent(distanceKey, new ConcurrentHashMap());
        }

        Map<String, String> matches = new HashMap((Map) this.propertiesMatched.get(distanceKey));
        Set<String> keys = new HashSet(properties.keySet());
        Iterator var7 = keys.iterator();

        while (var7.hasNext()) {
            String key = (String) var7.next();
            if (matches.containsKey(key)) {
                this.switchPropertyNames(properties, key, (String) matches.get(key));
            } else {
                String name = this.findPropertyName(bean, key);
                if (name != null) {
                    if (matches.containsValue(name)) {
                        throw new NotWritablePropertyException(cls, name, "Duplicate match with distance <= "
                                + this.distanceLimit + " found for this property in input keys: " + keys
                                + ". (Consider reducing the distance limit or changing the input key names to get a closer match.)");
                    }

                    matches.put(key, name);
                    this.switchPropertyNames(properties, key, name);
                }
            }
        }

        this.propertiesMatched.replace(distanceKey, new ConcurrentHashMap(matches));
        return properties;
    }

    private void switchPropertyNames(Properties properties, String oldName, String newName) {
        String value = properties.getProperty(oldName);
        properties.remove(oldName);
        properties.setProperty(newName, value);
    }

    private String findPropertyName(Object bean, String key) {
        if (bean == null) {
            return null;
        } else {
            Class<?> cls = bean.getClass();
            int index = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(key);
            String prefix;
            String suffix;
            String name;
            if (index > 0) {
                prefix = key.substring(0, index);
                suffix = key.substring(index + 1);
                name = this.findPropertyName(bean, prefix);
                if (name == null) {
                    return null;
                } else {
                    Object nestedValue = this.getPropertyValue(bean, name);
                    String nestedPropertyName = this.findPropertyName(nestedValue, suffix);
                    return nestedPropertyName != null ? name + "." + nestedPropertyName : null;
                }
            } else {
                name = null;
                int distance = 0;
                index = key.indexOf(91);
                if (index > 0) {
                    prefix = key.substring(0, index);
                    suffix = key.substring(index);
                } else {
                    prefix = key;
                    suffix = "";
                }

                for (; name == null && distance <= this.distanceLimit; ++distance) {
                    String[] candidates = PropertyMatches.forProperty(prefix, cls, distance).getPossibleMatches();
                    if (candidates.length == 1) {
                        String candidate = candidates[0];
                        if (candidate.equals(prefix)) {
                            name = key;
                        } else {
                            name = candidate + suffix;
                        }
                    }
                }

                return name;
            }
        }
    }

    private Object getPropertyValue(Object bean, String nestedName) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
        wrapper.setAutoGrowNestedPaths(true);
        Object nestedValue = wrapper.getPropertyValue(nestedName);
        if (nestedValue == null) {
            nestedValue = BeanUtils.instantiateClass(wrapper.getPropertyType(nestedName));
            wrapper.setPropertyValue(nestedName, nestedValue);
        }

        return nestedValue;
    }

    private static class DistanceHolder {

        private final Class<?> cls;

        private final int distance;

        DistanceHolder(Class<?> cls, int distance) {
            this.cls = cls;
            this.distance = distance;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DistanceHolder other = (DistanceHolder) obj;
            if (this.cls == null) {
                if (other.cls != null) {
                    return false;
                }
            } else if (!this.cls.equals(other.cls)) {
                return false;
            }
            return this.distance == other.distance;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.cls == null) ? 0 : this.cls.hashCode());
            result = prime * result + this.distance;
            return result;
        }

    }

}
