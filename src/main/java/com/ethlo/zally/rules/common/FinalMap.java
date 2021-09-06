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

import java.util.TreeMap;

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
 * Provides a nicer constructor for a TreeMap.
 * Example:
 * <PRE>
 * FinalMap&lt;String,Integer&gt; f=new FinalMap(
 * "a",1,
 * "b",2,
 * "c",3);
 * System.out.println(f.get("b"));
 * --&gt; 2
 * </PRE>
 */
public class FinalMap<T1 extends Comparable, T2> extends TreeMap<T1, T2>
{

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a FinalMap from an array that contains key/value sequences
     */
    @SuppressWarnings("unchecked")
    public FinalMap(Object... a)
    {
        super();
        for (int i = 0; i < a.length - 1; i += 2)
        {
            if (containsKey(a[i])) throw new RuntimeException("Duplicate key in FinalMap: " + a[i]);
            put((T1) a[i], (T2) a[i + 1]);
        }
    }

    /**
     * Test routine
     */
    public static void main(String[] args)
    {
        FinalMap<String, Integer> f = new FinalMap<String, Integer>("a", 1, "b", 2);
        System.out.println(f.get("b"));
    }
}
