/*
 * $Id$
 *
 * This file is part of the Cloud Services Integration Platform (CSIP),
 * a Model-as-a-Service framework, API, and application suite.
 *
 * 2012-2017, OMSLab, Colorado State University.
 *
 * OMSLab licenses this file to you under the MIT license.
 * See the LICENSE file in the project root for more information.
 */
package m.rhem.rhem04_surfacetextureclasses;

import csip.ModelDataService;
import csip.ServiceException;
import csip.annotations.Polling;
import csip.annotations.Resource;
import csip.utils.JSONUtils;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Path;
import csip.annotations.Description;
import csip.annotations.Name;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import rhem.utils.DBQueries;
import rhem.utils.DBResources;
import static rhem.utils.DBResources.CRDB;

/**
 * RHEM-04: Get RHEM Surface Texture Classes Choice List
 *
 * @version 1.0
 * @author rumpal
 */
@Name("RHEM-04: Get RHEM Surface Texture Classes Choice List.")
@Description("Get and return a list of RHEM surface soil texture classes.")
@Path("m/rhem/getsurftexclasses/1.0")
@Polling(first = 10000, next = 2000)
@Resource(from = DBResources.class)
public class V1_0 extends ModelDataService {

    private List<ChoiceList> choiceList = new ArrayList<>();

    @Override
    public void doProcess() throws ServiceException, ClassNotFoundException {
        try (Connection connection = getResourceJDBC(CRDB);
                Statement statement = connection.createStatement();) {
            try (ResultSet resultSet = statement.executeQuery(DBQueries.RHEM04Query01())) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("text_id");
                    String kind = "texture class";
                    int sequence = id;
                    String dataTextEntry = resultSet.getString("text_abreviation");
                    String label = resultSet.getString("text_label");
                    choiceList.add(new ChoiceList(id, kind, sequence, dataTextEntry, label));
                }
            }
        } catch (SQLException se) {
            throw new ServiceException(se);
        }
    }

    @Override
    public void postProcess() throws ServiceException {
        try {
            JSONArray resultArr = new JSONArray();
            for (ChoiceList list : choiceList) {
                JSONArray choiceListArr = new JSONArray();
                choiceListArr.put(JSONUtils.dataDesc("choice_id", list.getChoiceId(), "Choice Identifier"));
                choiceListArr.put(JSONUtils.dataDesc("choice_kind", list.getChoiceKind(), "Choice Kind"));
                choiceListArr.put(JSONUtils.dataDesc("choice_sequence", list.getChoiceSequence(), "Choice Sequence"));
                choiceListArr.put(JSONUtils.dataDesc("choice_data_entry_text", list.getChoiceDataEntryText(), "Choice Data Text Entry"));
                choiceListArr.put(JSONUtils.dataDesc("choice_label", list.getChoiceLabel(), "Choice Label"));
                resultArr.put(JSONUtils.dataDesc("surface_texture_classes", choiceListArr, "Surface Texture Classes Choice List"));
            }
            putResult("choice_lists", resultArr, "List");
        } catch (JSONException ex) {
            throw new ServiceException(ex);
        }
    }

    static class ChoiceList {

        protected int id;
        protected String kind;
        protected int sequence;
        protected String dataEntryText;
        protected String label;

        public ChoiceList(int id, String kind, int sequence,
                String dataEntryText, String label) {
            this.id = id;
            this.kind = kind;
            this.sequence = sequence;
            this.dataEntryText = dataEntryText;
            this.label = label;
        }

        public int getChoiceId() {
            return this.id;
        }

        public String getChoiceKind() {
            return this.kind;
        }

        public int getChoiceSequence() {
            return this.sequence;
        }

        public String getChoiceDataEntryText() {
            return this.dataEntryText;
        }

        public String getChoiceLabel() {
            return this.label;
        }
    }
}
