package customer.batchimportcat.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.CompareToBuilder;

import com.sap.cds.impl.builder.model.ElementRefImpl;
import com.sap.cds.impl.builder.model.ScalarFunctionCall;
import com.sap.cds.ql.FunctionCall;
// import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnElementRef;
import com.sap.cds.ql.cqn.CqnFunc;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnSelectListItem;
import com.sap.cds.ql.cqn.CqnSelectListValue;
import com.sap.cds.ql.cqn.CqnSortSpecification;
// import com.sap.cds.ql.cqn.transformation.CqnAggregateTransformation;
import com.sap.cds.ql.impl.SelectListValueBuilder;

// import io.vavr.Tuple;
// import reactor.util.function.Tuple7;

public class UnmanagedReportUtils {

    public static void sort(List<CqnSortSpecification> sortSpecificationList,
            List<? extends Map<String, ?>> entities) {

        // sort methods
        entities.sort((entity1, entity2) -> {
            // compare builder
            CompareToBuilder compareToBuilder = new CompareToBuilder();
            // loop cds sort specification
            for (CqnSortSpecification sort : sortSpecificationList) {

                // get element name
                CqnElementRef elementRef = (CqnElementRef) sort.value();
                // get sort key
                String sortKey = elementRef.displayName();
                String order = sort.order().sort;

                Object lhs, rhs;
                switch (order) {
                    case "asc":
                        lhs = entity1.get(sortKey); // get element in Map with sort key
                        rhs = entity2.get(sortKey);
                        break;
                    case "desc":
                        lhs = entity2.get(sortKey);
                        rhs = entity1.get(sortKey);
                        break;
                    default:
                        lhs = entity1.get(sortKey);
                        rhs = entity2.get(sortKey);
                        break;
                }
                // compare
                compareToBuilder.append(lhs, rhs);
            }
            // compare result
            return compareToBuilder.toComparison();
        });
    };

    public static List<? extends Map<String, ?>> getTopSkip(long top, long skip,
            List<? extends Map<String, ?>> entities) {
        int topInt = (int) top;
        int skipInt = (int) skip;

        if (topInt == 0) {
            return entities;
        } else {
            if (topInt + skipInt > entities.size()) {
                topInt = entities.size();
            } else {
                topInt = topInt + skipInt;
            }

            return entities.subList(skipInt, topInt);
        }

    }

    public static List<Map<String, Object>> aggregate(CqnSelect cqnSelect,
            List<? extends Map<String, ?>> entities) {

        List<CqnSelectListItem> columns = cqnSelect.columns();

        List<Map<String, String>> aggregateColumnList = new ArrayList<>();
        List<Map<String, String>> groupByColumnList = new ArrayList<>();

        columns.forEach(column -> {
            CqnSelectListValue selectListValue = (CqnSelectListValue) column.token();

            Map<String, String> mapCol = new HashMap<String, String>();

            // as field
            mapCol.put("alias", selectListValue.displayName());

            // differ field is aggregate or group by by class name
            Class<?> valueClass = selectListValue.value().getClass();
            switch (valueClass.getSimpleName()) {
                case "ScalarFunctionCall": // aggregate
                    // get aggregate function
                    ScalarFunctionCall<CqnFunc> function = (ScalarFunctionCall<CqnFunc>) selectListValue.value();

                    // function name
                    String funcName = function.getFunctionName();
                    mapCol.put("function", funcName);

                    // source field
                    if (function.args().size() > 0) {
                        CqnElementRef elementRef = (CqnElementRef) function.args().get(0);
                        mapCol.put("field", elementRef.displayName());
                    }

                    // if function name = singleValue(unit code/currency code) then it should be
                    // groupby field
                    if (funcName == "singleValue") {
                        groupByColumnList.add(mapCol);
                    } else {
                        aggregateColumnList.add(mapCol);
                    }
                    break;
                case "SingleSeg": // groupby
                    CqnElementRef groupbyField = (CqnElementRef) selectListValue.value();
                    mapCol.put("field", groupbyField.displayName());
                    groupByColumnList.add(mapCol);
                    break;
                default:
                    break;
            }

        });

        // Map<Map<String, ?>, ?> aggregateResults =
        // entities.stream().collect(Collectors.groupingBy(entity -> {
        List<Map.Entry<Map<String, Object>, Map<String, Object>>> aggregateResults = entities.stream()
                .collect(Collectors.groupingBy(entity -> {
                    // groub by fields
                    Map<String, ? super Object> groupByMap = new HashMap<>();
                    groupByColumnList.forEach(groupByColumn -> {
                        String field = groupByColumn.get("field");
                        groupByMap.put(field, entity.get(field));
                    });
                    return groupByMap;

                }, Collectors.collectingAndThen(Collectors.toList(), list -> {
                    // aggregate fields
                    Map<String, ? super Object> aggrMap = new HashMap<>();
                    aggregateColumnList.forEach(aggregateColumn -> {

                        switch (aggregateColumn.get("function")) {
                            case "SUM":
                                BigDecimal summary = list.stream().map((e) -> {
                                    return (BigDecimal) e.get(aggregateColumn.get("field"));
                                }).reduce(BigDecimal.ZERO, (subtotal, element) -> {
                                    return subtotal.add(element);
                                });
                                aggrMap.put(aggregateColumn.get("alias"), summary);
                                break;

                            case "COUNT":
                                long count = list.stream().collect(Collectors.counting());
                                aggrMap.put(aggregateColumn.get("alias"), count);
                                break;

                            default:
                                break;
                        }
                    });

                    return aggrMap;

                }))).entrySet().stream().toList();

        // Merge groupByFields and aggregateFields
        List<Map<String, Object>> results = new ArrayList<>();
        aggregateResults.forEach((result) -> {
            Map<String, ? super Object> map = new HashMap<>(result.getKey());
            map.putAll(result.getValue());
            results.add(map);
        });
        // return entities;
        return results;

    }

}
