/*-
 * ========================LICENSE_START=================================
 * Stdin/Stdout Helper for JUnit
 * %%
 * Copyright (C) 2020 - 2023 Pedro Alves
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
 * =========================LICENSE_END==================================
 */
package org.dropproject.stdinstdoutjunithelper;

public final class SystemUtil {

    private static final String SYSTEM_OS_NAME = "os.name";

    private static final String WINDOWS = "WINDOWS";

    public static String getOperatingSystemName() {
        return System.getProperty(SYSTEM_OS_NAME);
    }

    public static boolean isWindows() {
        return getOperatingSystemName().toUpperCase().startsWith(WINDOWS);
    }

}
