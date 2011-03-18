/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.resources.configurations;

import com.android.resources.ResourceEnum;
import com.android.resources.ScreenOrientation;

/**
 * Resource Qualifier for Screen Orientation.
 */
public final class ScreenOrientationQualifier extends EnumBasedResourceQualifier {

    public static final String NAME = "Screen Orientation";

    private ScreenOrientation mValue = null;

    public ScreenOrientationQualifier() {
    }

    public ScreenOrientationQualifier(ScreenOrientation value) {
        mValue = value;
    }

    public ScreenOrientation getValue() {
        return mValue;
    }

    @Override
    ResourceEnum getEnumValue() {
        return mValue;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getShortName() {
        return "Orientation";
    }

    @Override
    public boolean checkAndSet(String value, FolderConfiguration config) {
        ScreenOrientation orientation = ScreenOrientation.getEnum(value);
        if (orientation != null) {
            ScreenOrientationQualifier qualifier = new ScreenOrientationQualifier(orientation);
            config.setScreenOrientationQualifier(qualifier);
            return true;
        }

        return false;
    }
}
