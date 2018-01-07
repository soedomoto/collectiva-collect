/*
 * Copyright 2017 Nafundi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.odk.collect.android.dto;

/**
 * This class represents a single row from the forms table which is located in
 * {@link org.odk.collect.android.provider.FormsProvider#DATABASE_NAME}
 * For more information about this pattern go to https://en.wikipedia.org/wiki/Data_transfer_object
 * Objects of this class are created using builder pattern: https://en.wikipedia.org/wiki/Builder_pattern
 */
public class Form {
    private String mDisplayName;
    private String mDescription;
    private String mJrFormId;
    private String mJrVersion;
    private String mFormFilePath;
    private String mSubmissionUri;
    private String mBASE64RSAPublicKey;
    private String mDisplaySubtext;
    private String mMD5Hash;
    private Long mDate;
    private String mJrCacheFilePath;
    private String mFormMediaPath;
    private String mLanguage;

    private Form(Form.Builder builder) {
        mDisplayName = builder.mDisplayName;
        mDescription = builder.mDescription;
        mJrFormId = builder.mJrFormId;
        mJrVersion = builder.mJrVersion;
        mFormFilePath = builder.mFormFilePath;
        mSubmissionUri = builder.mSubmissionUri;
        mBASE64RSAPublicKey = builder.mBASE64RSAPublicKey;
        mDisplaySubtext = builder.mDisplaySubtext;
        mMD5Hash = builder.mMD5Hash;
        mDate = builder.mDate;
        mJrCacheFilePath = builder.mJrCacheFilePath;
        mFormMediaPath = builder.mFormMediaPath;
        mLanguage = builder.mLanguage;
    }

    public static class Builder {
        private String mDisplayName;
        private String mDescription;
        private String mJrFormId;
        private String mJrVersion;
        private String mFormFilePath;
        private String mSubmissionUri;
        private String mBASE64RSAPublicKey;
        private String mDisplaySubtext;
        private String mMD5Hash;
        private Long mDate;
        private String mJrCacheFilePath;
        private String mFormMediaPath;
        private String mLanguage;

        public Builder displayName(String displayName) {
            mDisplayName = displayName;
            return this;
        }

        public Builder description(String description) {
            mDescription = description;
            return this;
        }

        public Builder jrFormId(String jrFormId) {
            mJrFormId = jrFormId;
            return this;
        }

        public Builder jrVersion(String jrVersion) {
            mJrVersion = jrVersion;
            return this;
        }


        public Builder formFilePath(String formFilePath) {
            mFormFilePath = formFilePath;
            return this;
        }

        public Builder submissionUri(String submissionUri) {
            mSubmissionUri = submissionUri;
            return this;
        }


        public Builder base64RSAPublicKey(String base64RSAPublicKey) {
            mBASE64RSAPublicKey = base64RSAPublicKey;
            return this;
        }

        public Builder displaySubtext(String displaySubtext) {
            mDisplaySubtext = displaySubtext;
            return this;
        }

        public Builder md5Hash(String md5Hash) {
            mMD5Hash = md5Hash;
            return this;
        }

        public Builder date(Long date) {
            mDate = date;
            return this;
        }

        public Builder jrCacheFilePath(String jrCacheFilePath) {
            mJrCacheFilePath = jrCacheFilePath;
            return this;
        }

        public Builder formMediaPath(String formMediaPath) {
            mFormMediaPath = formMediaPath;
            return this;
        }

        public Builder language(String language) {
            mLanguage = language;
            return this;
        }

        public Form build() {
            return new Form(this);
        }
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getJrFormId() {
        return mJrFormId;
    }

    public String getJrVersion() {
        return mJrVersion;
    }

    public String getFormFilePath() {
        return mFormFilePath;
    }

    public String getSubmissionUri() {
        return mSubmissionUri;
    }

    public String getBASE64RSAPublicKey() {
        return mBASE64RSAPublicKey;
    }

    public String getDisplaySubtext() {
        return mDisplaySubtext;
    }

    public String getMD5Hash() {
        return mMD5Hash;
    }

    public Long getDate() {
        return mDate;
    }

    public String getJrCacheFilePath() {
        return mJrCacheFilePath;
    }

    public String getFormMediaPath() {
        return mFormMediaPath;
    }

    public String getLanguage() {
        return mLanguage;
    }
}
