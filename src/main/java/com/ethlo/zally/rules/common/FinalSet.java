package com.ethlo.zally.rules.common;

/*-
 * #%L
 * zally-maven-plugin
 * %%
 * Copyright (C) 2021 Morten Haraldsen (ethlo)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Set;
import java.util.Spliterator;

/**
 * Copyright 2016 Fabian M. Suchanek
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * This class provides a very simple container implementation with zero
 * overhead. A FinalSet bases on a sorted, unmodifiable array. The constructor
 * can either be called with a sorted unmodifiable array (default constructor)
 * or with an array that can be cloned and sorted beforehand if desired.
 * Example:
 *
 * <PRE>
 * FinalSet&lt;String&gt; f=new FinalSet("a","b","c");
 * // equivalently:
 * //   FinalSet&lt;String&gt; f=new FinalSet(new String[]{"a","b","c"});
 * //   FinalSet&lt;String&gt; f=new FinalSet(SHALLNOTBECLONED,ISSORTED,"a","b","c");
 * System.out.println(f.get(1));
 * --&gt; b
 * </PRE>
 */
public class FinalSet<T extends Comparable<?>> extends AbstractList<T> implements Set<T>
{

    /**
     * Holds the data, must be sorted
     */
    public T[] data;

    /**
     * Constructs a FinalSet from an array, clones the array if indicated.
     */
    @SuppressWarnings("unchecked")
    public FinalSet(boolean clone, T... a)
    {
        if (clone)
        {
            Comparable<?>[] b = new Comparable[a.length];
            System.arraycopy(a, 0, b, 0, a.length);
            a = (T[]) b;
        }
        Arrays.sort(a);
        data = a;
    }

    /**
     * Constructs a FinalSet from an array that does not need to be cloned
     */
    public FinalSet(T... a)
    {
        this(false, a);
    }

    /**
     * Tells whether x is in the container
     */
    public boolean contains(T x)
    {
        return (Arrays.binarySearch(data, x) >= 0);
    }

    /**
     * Returns the position in the array or -1
     */
    public int indexOf(T x)
    {
        int r = Arrays.binarySearch(data, x);
        return (r >= 0 ? r : -1);
    }

    /**
     * Returns the element at position i
     */
    @Override
    public T get(int i)
    {
        return (data[i]);
    }

    /**
     * Returns the number of elements in this FinalSet
     */
    @Override
    public int size()
    {
        return (data.length);
    }

    /* Choosing default implementation. Comment out this function to compile with Java 6/7 */
    public Spliterator<T> spliterator()
    {
        return super.spliterator();
    }
}
