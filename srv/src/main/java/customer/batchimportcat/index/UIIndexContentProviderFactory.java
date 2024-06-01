package customer.batchimportcat.index;

import java.io.PrintWriter;

import com.sap.cds.adapter.IndexContentProvider;
import com.sap.cds.adapter.IndexContentProviderFactory;

// import com.sap.cds.ad

public class UIIndexContentProviderFactory implements IndexContentProviderFactory {

    @Override
    public IndexContentProvider create() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'create'");
        // return new UIIndexContentProviderFactory();
        return new UIIndexContentProvider();
    }

    @Override
    public boolean isEnabled() {
        // TODO Auto-generated method stub
        // throw new UnsupportedOperationException("Unimplemented method 'isEnabled'");
        return true;
    }

    private static class UIIndexContentProvider implements IndexContentProvider {

        private static final String ENDPOINT_START = "" +
                "                <ul>\n";

        private static final String ENDPOINT = "" +
                "                    <li>\n" +
                "                        <a href=\"%s\">%s</a>\n" +
                "                    </li>\n";

        private static final String ENDPOINT_END = "" +
                "                </ul>\n";

        @Override
        public String getSectionTitle() {
            // TODO Auto-generated method stub
            // throw new UnsupportedOperationException("Unimplemented method
            // 'getSectionTitle'");
            return "UI endpoints";
        }

        @Override
        public void writeContent(PrintWriter writer, String contextPath) {
            // TODO Auto-generated method stub
            // throw new UnsupportedOperationException("Unimplemented method 'writeContent'");
            writer.print(ENDPOINT_START);
			writer.printf(ENDPOINT, contextPath + "/fiori.html", "Fiori UI");
			// writer.printf(ENDPOINT, contextPath + "/vue/index.html", "Vue.js UI");
			// writer.printf(ENDPOINT, contextPath + "/swagger/index.html", "Swagger UI");
			writer.print(ENDPOINT_END);
        }

    }

}
