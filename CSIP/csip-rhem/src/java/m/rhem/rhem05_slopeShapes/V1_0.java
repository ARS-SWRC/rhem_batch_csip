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
package m.rhem.rhem05_slopeShapes;

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
import javax.ws.rs.Path;
import csip.annotations.Description;
import csip.annotations.Name;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import rhem.utils.DBQueries;
import rhem.utils.DBResources;
import static rhem.utils.DBResources.CRDB;

/**
 * RHEM-05: Get RHEM Slope Shape Choice List
 *
 * @version 1.0
 * @author rumpal
 */
@Name("RHEM-05: Get RHEM Slope Shape Choice List.")
@Description("Get and return a list of RHEM slope shapes.")
@Path("m/rhem/getslopeshapes/1.0")
@Polling(first = 10000, next = 2000)
@Resource(from = DBResources.class)
public class V1_0 extends ModelDataService {

    private ArrayList<ChoiceList> choiceList = new ArrayList<>();

    @Override
    public void doProcess() throws ServiceException {
        choiceList = new ArrayList<>();
        try (Connection conn = getResourceJDBC(CRDB);
                Statement statement = conn.createStatement();) {
            try (ResultSet resultSet = statement.executeQuery(DBQueries.RHEM05Query01())) {
                while (resultSet.next()) {
                    int id = resultSet.getInt("choice_id");
                    String kind = "slope shape";
                    int sequence = id;
                    String label = resultSet.getString("choice_label");
                    choiceList.add(new ChoiceList(id, kind, sequence, label));
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
                choiceListArr.put(JSONUtils.dataDesc("choice_label", list.getChoiceLabel(), "Choice Label"));
                resultArr.put(JSONUtils.dataDesc("slope_shape", choiceListArr, "Slope Shape"));
            }
            putResult("choice_lists", resultArr, "Slope Shape Choice List");
        } catch (JSONException ex) {
            throw new ServiceException(ex);
        }
    }

    static class ChoiceList {

        protected int id;
        protected String kind;
        protected int sequence;
        protected String label;

        public ChoiceList(int id, String kind, int sequence, String label) {
            this.id = id;
            this.kind = kind;
            this.sequence = sequence;
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

        public String getChoiceLabel() {
            return this.label;
        }
    }
}
