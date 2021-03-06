/*
 * Grakn - A Distributed Semantic Database
 * Copyright (C) 2016  Grakn Labs Limited
 *
 * Grakn is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grakn is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grakn. If not, see <http://www.gnu.org/licenses/gpl.txt>.
 */

package ai.grakn.engine.loader;

import ai.grakn.GraknGraph;
import ai.grakn.engine.postprocessing.GraphMutators;
import ai.grakn.engine.tasks.BackgroundTask;
import ai.grakn.engine.tasks.TaskCheckpoint;
import ai.grakn.engine.tasks.TaskConfiguration;
import ai.grakn.graql.Graql;
import ai.grakn.graql.Query;
import ai.grakn.graql.QueryBuilder;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import mjson.Json;

import static ai.grakn.util.ErrorMessage.ILLEGAL_ARGUMENT_EXCEPTION;
import static ai.grakn.util.ErrorMessage.READ_ONLY_QUERY;
import static ai.grakn.util.REST.Request.TASK_LOADER_MUTATIONS;

/**
 * Task that will mutate data in a graph. It uses the engine running on the
 * engine executing the task.
 *
 * The task will then submit all modified concepts for post processing.
 *
 * @author Alexandra Orth
 */
public class MutatorTask implements BackgroundTask {

    private final QueryBuilder builder = Graql.withoutGraph().infer(false);

    @Override
    public boolean start(Consumer<TaskCheckpoint> saveCheckpoint, TaskConfiguration configuration) {
        Collection<Query> inserts = getInserts(configuration);
        GraphMutators.runBatchMutationWithRetry(configuration, (graph) ->
                insertQueriesInOneTransaction(graph, inserts)
        );

        return true;
    }

    @Override
    public boolean stop() {
        throw new UnsupportedOperationException("Loader task cannot be stopped while in progress");
    }

    @Override
    public void pause() {
        throw new UnsupportedOperationException("Loader task cannot be paused");
    }

    @Override
    public boolean resume(Consumer<TaskCheckpoint> saveCheckpoint, TaskCheckpoint lastCheckpoint) {
        throw new UnsupportedOperationException("Loader task cannot be resumed");
    }

    /**
     * Execute the given queries against the given graph. Return if the operation was successfully completed.
     * @param graph grakn graph in which to insert the data
     * @param inserts graql queries to insert into the graph
     * @return true if the data was inserted, false otherwise
     */
    private boolean insertQueriesInOneTransaction(GraknGraph graph, Collection<Query> inserts) {
        graph.showImplicitConcepts(true);

        inserts.forEach(q -> q.withGraph(graph).execute());

        // commit the transaction
        //TODO This commit uses the rest API, it shouldn't
        graph.commit();

        return true;
    }


    /**
     * Extract mutate queries from a configuration object
     * @param configuration JSONObject containing configuration
     * @return graql queries from the configuration
     */
    private Collection<Query> getInserts(TaskConfiguration configuration){
        if(configuration.json().has(TASK_LOADER_MUTATIONS)){
            return configuration.json().at(TASK_LOADER_MUTATIONS).asJsonList().stream()
                    .map(Json::asString)
                    .map(builder::<Query<?>>parse)
                    .map(query -> {
                        if (query.isReadOnly()) {
                            throw new IllegalArgumentException(READ_ONLY_QUERY.getMessage(query.toString()));
                        }
                        return query;
                    })
                    .collect(Collectors.toList());
        }

        throw new IllegalArgumentException(ILLEGAL_ARGUMENT_EXCEPTION.getMessage("No inserts", configuration));
    }
}
