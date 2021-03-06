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

package io.shardingsphere.orchestration.internal.config;

import io.shardingsphere.core.constant.ShardingConstant;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public final class ConfigurationNodeTest {
    
    private final ConfigurationNode configurationNode = new ConfigurationNode("test");
    
    @Test
    public void assertGetDataSourcePath() {
        assertThat(configurationNode.getDataSourcePath(ShardingConstant.LOGIC_SCHEMA_NAME), is("/test/config/sharding_db/datasource"));
    }
    
    @Test
    public void assertGetRulePath() {
        assertThat(configurationNode.getRulePath(ShardingConstant.LOGIC_SCHEMA_NAME), is("/test/config/sharding_db/rule"));
    }
    
    @Test
    public void assertGetConfigMapPath() {
        assertThat(configurationNode.getConfigMapPath(ShardingConstant.LOGIC_SCHEMA_NAME), is("/test/config/sharding_db/configmap"));
    }
    
    @Test
    public void assertGetPropsPath() {
        assertThat(configurationNode.getPropsPath(ShardingConstant.LOGIC_SCHEMA_NAME), is("/test/config/sharding_db/props"));
    }
}
