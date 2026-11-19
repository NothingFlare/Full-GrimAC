package com.alessiodp.libby;

public class Library {
    private final String groupId;
    private final String artifactId;
    private final String version;

    public Library(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String groupId;
        private String artifactId;
        private String version;

        public Builder groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder artifactId(String artifactId) {
            this.artifactId = artifactId;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder relocate(String pattern, String replacement) {
            return this;
        }

        public Builder resolveTransitiveDependencies(boolean resolve) {
            return this;
        }

        public Builder excludeTransitiveDependency(String groupId, String artifactId) {
            return this;
        }

        public Library build() {
            return new Library(groupId, artifactId, version);
        }
    }
}
