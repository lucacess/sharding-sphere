/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.dbtest.engine;

import com.google.common.base.Splitter;
import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.dbtest.cases.assertion.IntegrateTestCasesLoader;
import io.shardingsphere.dbtest.cases.assertion.dql.DQLIntegrateTestCase;
import io.shardingsphere.dbtest.cases.assertion.dql.DQLIntegrateTestCaseAssertion;
import io.shardingsphere.dbtest.cases.dataset.expected.dataset.ExpectedDataSetsRoot;
import io.shardingsphere.dbtest.common.SQLValue;
import io.shardingsphere.dbtest.env.DatabaseTypeEnvironment;
import io.shardingsphere.test.sql.SQLCaseType;
import io.shardingsphere.test.sql.SQLCasesLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public final class DQLIntegrateTest extends BaseIntegrateTest {
    
    private static SQLCasesLoader sqlCasesLoader = SQLCasesLoader.getInstance();
    
    private static IntegrateTestCasesLoader integrateTestCasesLoader = IntegrateTestCasesLoader.getInstance();
    
    private final DQLIntegrateTestCaseAssertion assertion;
    
    public DQLIntegrateTest(final String sqlCaseId, final String path, final DQLIntegrateTestCaseAssertion assertion,
                            final DatabaseTypeEnvironment databaseTypeEnvironment, final SQLCaseType caseType) throws IOException, JAXBException, SQLException {
        super(sqlCaseId, path, assertion, databaseTypeEnvironment, caseType);
        this.assertion = assertion;
    }
    
    @Parameters(name = "{0} -> {2} -> {3} -> {4}")
    public static Collection<Object[]> getParameters() {
        // TODO sqlCasesLoader size should eq integrateTestCasesLoader size
        // assertThat(sqlCasesLoader.countAllSupportedSQLCases(), is(integrateTestCasesLoader.countAllDataSetTestCases()));
        Collection<Object[]> result = new LinkedList<>();
        for (Object[] each : sqlCasesLoader.getSupportedSQLTestParameters(Arrays.<Enum>asList(DatabaseType.values()), DatabaseType.class)) {
            String sqlCaseId = each[0].toString();
            DatabaseType databaseType = (DatabaseType) each[1];
            SQLCaseType caseType = (SQLCaseType) each[2];
            DQLIntegrateTestCase integrateTestCase = integrateTestCasesLoader.getDQLIntegrateTestCase(sqlCaseId);
            // TODO remove when transfer finished
            if (null == integrateTestCase) {
                continue;
            }
            if (getDatabaseTypes(integrateTestCase.getDatabaseTypes()).contains(databaseType)) {
                result.addAll(getParameters(databaseType, caseType, integrateTestCase));
            }
        }
        return result;
    }
    
    @Before
    public void insertData() throws SQLException, ParseException {
        if (getDatabaseTypeEnvironment().isEnabled()) {
            getDataSetEnvironmentManager().initialize(false);
        }
    }
    
    @Test
    public void assertExecuteQuery() throws JAXBException, IOException, SQLException, ParseException {
        if (!getDatabaseTypeEnvironment().isEnabled()) {
            return;
        }
        try (Connection connection = getDataSource().getConnection()) {
            if (SQLCaseType.Literal == getCaseType()) {
                try (
                        Statement statement = connection.createStatement();
                        ResultSet resultSet = statement.executeQuery(generateSQL(getSql(), assertion.getSQLValues()))) {
                    assertResultSet(resultSet);
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement(getSql().replaceAll("%s", "?"))) {
                    for (SQLValue each : assertion.getSQLValues()) {
                        preparedStatement.setObject(each.getIndex(), each.getValue());
                    }
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        assertResultSet(resultSet);
                    }
                }
            }
        }
    }
    
    @Test
    public void assertExecute() throws JAXBException, IOException, SQLException, ParseException {
        if (!getDatabaseTypeEnvironment().isEnabled()) {
            return;
        }
        try (Connection connection = getDataSource().getConnection()) {
            if (SQLCaseType.Literal == getCaseType()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(generateSQL(getSql(), assertion.getSQLValues()));
                    try (ResultSet resultSet = statement.getResultSet()) {
                        assertResultSet(resultSet);
                    }
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement(getSql().replaceAll("%s", "?"))) {
                    for (SQLValue each : assertion.getSQLValues()) {
                        preparedStatement.setObject(each.getIndex(), each.getValue());
                    }
                    assertTrue("Not a query statement.", preparedStatement.execute());
                    try (ResultSet resultSet = preparedStatement.getResultSet()) {
                        assertResultSet(resultSet);
                    }
                }
            }
        }
    }
    
    private static String generateSQL(final String sql, final Collection<SQLValue> sqlValues) {
        if (null == sqlValues) {
            return sql;
        }
        String result = sql;
        for (SQLValue each : sqlValues) {
            result = Pattern.compile("%s", Pattern.LITERAL).matcher(result)
                    .replaceFirst(Matcher.quoteReplacement(each.getValue() instanceof String ? "'" + each.getValue() + "'" : each.getValue().toString()));
        }
        return result;
    }
    
    private void assertResultSet(final ResultSet resultSet) throws SQLException, JAXBException, IOException {
        ExpectedDataSetsRoot expected;
        try (FileReader reader = new FileReader(getExpectedDataFile())) {
            expected = (ExpectedDataSetsRoot) JAXBContext.newInstance(ExpectedDataSetsRoot.class).createUnmarshaller().unmarshal(reader);
        }
        List<String> expectedColumnNames = Splitter.on(",").trimResults().splitToList(expected.getColumns().getValues());
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        assertThat(columnCount, is(expectedColumnNames.size()));
        int index = 1;
        for (String each : expectedColumnNames) {
            assertThat(metaData.getColumnLabel(index++), is(each));
        }
        int count = 0;
        while (resultSet.next()) {
            List<String> values = Splitter.on(",").trimResults().splitToList(expected.getDataSetRows().get(count).getValues());
            int valueIndex = 0;
            for (String each : values) {
                if (Types.DATE == metaData.getColumnType(valueIndex + 1)) {
                    assertThat(new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(resultSet.getDate(valueIndex + 1).getTime())), is(each));
                    assertThat(new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(resultSet.getDate(expectedColumnNames.get(valueIndex)).getTime())), is(each));
                } else {
                    assertThat(String.valueOf(resultSet.getObject(valueIndex + 1)), is(each));
                    assertThat(String.valueOf(resultSet.getObject(expectedColumnNames.get(valueIndex))), is(each));
                }
                valueIndex++;
            }
            count++;
        }
        assertThat(count, is(expected.getDataSetRows().size()));
    }
}