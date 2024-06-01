package customer.batchimportcat.batch.itemReaders.columnNameExtrators;

import java.util.Arrays;

import org.springframework.batch.extensions.excel.Sheet;
import org.springframework.batch.extensions.excel.support.rowset.ColumnNameExtractor;

import com.sap.cds.reflect.CdsElement;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsStructuredType;

import customer.batchimportcat.batch.exceptions.BatchExceptionsUtil;

public class CdsColumnNameExtrators implements ColumnNameExtractor {
    private String[] columnNames;
    private final String cdsName;

    // @Autowired
    private CdsModel cdsModel;

    @Override
    public String[] getColumnNames(Sheet sheet) {
        // TODO Auto-generated method stub
        return (String[]) Arrays.copyOf(this.columnNames, this.columnNames.length);
    }

    public CdsColumnNameExtrators(String cdsName, CdsModel cdsModel) {
        this.cdsName = cdsName;
        this.cdsModel = cdsModel;

        // get fields by Property

    }

    public void getColumnNameByCDS() {
        CdsStructuredType structuredType;
        try {
            structuredType = this.cdsModel.findStructuredType(cdsName).get();
            columnNames = structuredType.elements().map(CdsElement::getName).toArray(size -> new String[size]);

        } catch (Exception e) {
            // TODO: handle exception
            throw BatchExceptionsUtil.getBatchConfigNotFoundBecauseNoStruct(e);
        }

    }

    // public void setColu

}
