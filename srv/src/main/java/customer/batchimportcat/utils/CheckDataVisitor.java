package customer.batchimportcat.utils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
// import java.util.stream.Collector;
import java.util.stream.Collectors;
import com.sap.cds.ql.cqn.CqnComparisonPredicate;
import com.sap.cds.ql.cqn.CqnConnectivePredicate;
import com.sap.cds.ql.cqn.CqnContainmentTest;
import com.sap.cds.ql.cqn.CqnElementRef;
import com.sap.cds.ql.cqn.CqnInPredicate;
import com.sap.cds.ql.cqn.CqnLiteral;
import com.sap.cds.ql.cqn.CqnSortSpecification;
import com.sap.cds.ql.cqn.CqnVisitor;

public class CheckDataVisitor implements CqnVisitor {
    private Map<String, Object> data;
    private final Deque<Object> stack = new ArrayDeque<>();

    private List<Map<String, Object>> dataList;

    public CheckDataVisitor(Map<String, Object> data) {
        this.data = data;
    }

    public CheckDataVisitor(List<Map<String, Object>> dataList) {
        this.dataList = dataList;
    }

    /*
     * return record if match all conditions
     */
    public boolean matches() {
        return (Boolean) stack.pop();
    }

    @Override
    public void visit(CqnSortSpecification sortSpecification) {
        // sortSpecification.
    }

    // data value by element ID
    @Override
    public void visit(CqnElementRef ref) {
        Object dataValue = data.get(ref.displayName());
        if (dataValue != null) {
            stack.push(dataValue);
        } else {
            stack.push("null");
        }
    }

    // condition value
    @Override
    public void visit(CqnLiteral<?> literal) {
        stack.push(literal.value());
    }

    /*
     * in condition
     */
    @Override
    public void visit(CqnInPredicate in) {
        List<Object> values = in.values().stream()
                .map(v -> stack.pop()).collect(Collectors.toList());
        Object value = stack.pop();
        stack.push(values.stream().anyMatch(value::equals));
    }

    /*
     * comparison condition
     */
    @Override
    @SuppressWarnings("unchecked")
    public void visit(CqnComparisonPredicate comparison) {

        Comparable<Object> rhs = (Comparable<Object>) stack.pop();
        Comparable<Object> lhs = (Comparable<Object>) stack.pop();
        int cmp = lhs.compareTo(rhs);
        switch (comparison.operator()) {
            case EQ:
            case IS:
                stack.push(cmp == 0);
                break;
            case GT:
                stack.push(cmp > 0);
                break;
            case LT:
                stack.push(cmp < 0);
                break;
            case GE:
                stack.push(cmp >= 0);
                break;
            case LE:
                stack.push(cmp <= 0);
                break;
            case IS_NOT:
            case NE:
                stack.push(!(cmp == 0));
                break;
            default:
                stack.push(true);
                break;
        }
    }

    /*
     * contains condition
     */
    @Override
    public void visit(CqnContainmentTest test) {
        String substr = (String) stack.pop();
        String value = (String) stack.pop();
        stack.push(value.contains(substr));
    }

    /*
     * Connect Condition Results(Boolean) from same brackets ( a = 0 or a = 1 or a =
     * 2) (a = 0 and b = 1 and c = 2)
     */
    @Override
    public void visit(CqnConnectivePredicate connect) {

        Deque<Boolean> booleansInThisBrackets = new ArrayDeque<>();
        for (int i = 0; i < connect.predicates().size(); i++) {
            booleansInThisBrackets.push((Boolean) stack.pop());
        }
        stack.push(getCombine(booleansInThisBrackets, connect.operator().symbol));

        // switch (connect.operator()) {
        // case AND:
        // stack.push(lhs && rhs);
        // break;
        // case OR:
        // stack.push(lhs || rhs);
        // break;
        // }
    }

    public boolean getCombine(Deque<Boolean> boolList, String operator) {
        if (boolList.size() < 2) {
            return boolList.pop();
        }
        Boolean rhs = boolList.pop();
        Boolean lhs = boolList.pop();
        switch (operator) {
            case "and":
                boolList.push(lhs && rhs);
                break;
            case "or":
                boolList.push(lhs || rhs);
                break;
        }
        return getCombine(boolList, operator);
    }

}
