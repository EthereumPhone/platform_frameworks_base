package com.android.server;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StructuredData {
    public static class Entry {
        private final String name;
        private final String type;

        public Entry() {
            super();
            name = null;
            type = null;
        }

        @JsonCreator
        public Entry(
                @JsonProperty("name") String name,
                @JsonProperty("type") String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }
    }

    public static class EIP712Domain {
        private final String name;
        private final String version;
        private final String chainId;
        private final String verifyingContract;
        private final String salt;

        public EIP712Domain() {
            super();
            name = null;
            version = null;
            chainId = null;
            verifyingContract = null;
            salt = null;
        }

        @JsonCreator
        public EIP712Domain(
                @JsonProperty("name") String name,
                @JsonProperty("version") String version,
                @JsonProperty("chainId") String chainId,
                @JsonProperty("verifyingContract") String verifyingContract,
                @JsonProperty("salt") String salt) {
            this.name = name;
            this.version = version;
            this.chainId = chainId;
            this.verifyingContract = verifyingContract;
            this.salt = salt;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public String getChainId() {
            return chainId;
        }

        public String getVerifyingContract() {
            return verifyingContract;
        }

        public String getSalt() {
            return salt;
        }
    }

    public static class EIP712Message {
        private final HashMap<String, List<Entry>> types;
        private final String primaryType;
        private final Object message;
        private final EIP712Domain domain;

        public EIP712Message() {
            super();
            types = null;
            primaryType = null;
            message = null;
            domain = null;
        }

        @JsonCreator
        public EIP712Message(
                @JsonProperty("types") HashMap<String, List<Entry>> types,
                @JsonProperty("primaryType") String primaryType,
                @JsonProperty("message") Object message,
                @JsonProperty("domain") EIP712Domain domain) {
            this.types = types;
            this.primaryType = primaryType;
            this.message = message;
            this.domain = domain;
        }

        public HashMap<String, List<Entry>> getTypes() {
            return types;
        }

        public String getPrimaryType() {
            return primaryType;
        }

        public Object getMessage() {
            return message;
        }

        public EIP712Domain getDomain() {
            return domain;
        }

        @Override
        public String toString() {
            return "EIP712Message{"
                    + "primaryType='"
                    + this.primaryType
                    + '\''
                    + ", message='"
                    + this.message
                    + '\''
                    + '}';
        }
    }
}