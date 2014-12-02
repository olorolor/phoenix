package org.apache.phoenix.calcite;

import org.apache.calcite.linq4j.AbstractEnumerable;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.phoenix.compile.ColumnProjector;
import org.apache.phoenix.compile.QueryPlan;
import org.apache.phoenix.compile.RowProjector;
import org.apache.phoenix.iterate.ResultIterator;
import org.apache.phoenix.schema.tuple.Tuple;

import java.sql.SQLException;

/**
 * Methods used by code generated by Calcite.
 */
public class CalciteRuntime {
    public static Enumerable<Object[]> toEnumerable2(final ResultIterator iterator, final RowProjector rowProjector) {
        return new AbstractEnumerable<Object[]>() {
            @Override
            public Enumerator<Object[]> enumerator() {
                return toEnumerator(iterator, rowProjector);
            }
        };
    }

    public static Enumerable<Object[]> toEnumerable(final QueryPlan plan) {
        try {
            return toEnumerable2(plan.iterator(), plan.getProjector());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Enumerator<Object[]> toEnumerator(final ResultIterator iterator, final RowProjector rowProjector) {
        return new Enumerator<Object[]>() {
            Object[] current = new Object[rowProjector.getColumnCount()];
            private final ImmutableBytesWritable ptr = new ImmutableBytesWritable();

            @Override
            public Object[] current() {
                return current;
            }

            @Override
            public boolean moveNext() {
                try {
                    final Tuple tuple = iterator.next();
                    if (tuple == null) {
                        current = null;
                        return false;
                    }
                    for (int i = 0; i < current.length; i++) {
                        ColumnProjector projector = rowProjector.getColumnProjector(i);
                    	current[i] = projector.getValue(tuple, projector.getExpression().getDataType(), ptr);
                    }
                    return true;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void reset() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() {
                try {
                    iterator.close();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
