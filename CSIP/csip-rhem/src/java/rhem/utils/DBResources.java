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
package rhem.utils;

import csip.annotations.Resource;
import static csip.annotations.ResourceType.EXECUTABLE;
import static csip.annotations.ResourceType.JDBC;
import static rhem.utils.DBResources.*;

@Resource(type = JDBC, file = "${crdb.db}", id = CRDB, env = {
    "removeAbandoned=false", "defaultReadOnly=true", "defaultAutoCommit=false",
    "jdbcInterceptors=org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
    + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer;"
    + "org.apache.tomcat.jdbc.pool.interceptor.ResetAbandonedTimer"
})
@Resource(type = JDBC, file = "${esd.db}", id = ESD, env = {
    "removeAbandoned=false", "defaultReadOnly=true", "defaultAutoCommit=false",
    "jdbcInterceptors=org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
    + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer;"
    + "org.apache.tomcat.jdbc.pool.interceptor.ResetAbandonedTimer"
})

@Resource(type = JDBC, file = "${sdm.rest.db}", id = SDM_REST, env = {
    "driverClassName=csip.sdm.SDMDriver", "validationQuery=SELECT 1 FROM mapunit;",
    "maxWait=300000", "testOnBorrow=false"
})

@Resource(type = EXECUTABLE, file = "/bin/${csip.arch}/rhem_v23.exe", id = RHEM_EXE)
@Resource(type = EXECUTABLE, file = "/bin/${csip.arch}/rhem_ra.exe", id = RHEM_RA_EXE)

/**
 * All external RHEM service resources.
 */
public interface DBResources {

    static final String CRDB = "crdb";
    static final String ESD = "esd";
    static final String SDM_REST = "SDMDriver";

    static final String RHEM_EXE = "rhem.exe";
    static final String RHEM_RA_EXE = "rhem_ra.exe";
}
