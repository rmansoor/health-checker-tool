package com.epam.facade.model.context;

import com.epam.facade.model.cluster.receiver.InvalidBuildParamsException;
import com.epam.util.common.CheckingParamsUtil;

import java.util.Objects;
import java.util.function.Consumer;

public class ContextKey {
    private String majorKey;
    private String minorKey;
    private Class<?> holderClass;

    private ContextKey() {}

    public String getMajorKey() {
        return majorKey;
    }

    public String getMinorKey() {
        return minorKey;
    }

    public Class<?> getHolderClass() {
        return holderClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContextKey that = (ContextKey) o;

        return majorKey.equals(that.majorKey) && minorKey.equals(that.minorKey);
//                && holderClass.equals(that.holderClass);
    }

    @Override
    public int hashCode() {
        int result = majorKey.hashCode();
        result = 31 * result + minorKey.hashCode();
//        result = 31 * result + holderClass.hashCode();
        return result;
    }

    public static class ContextKeyBuilder {
        private ContextKey contextKey;

        private ContextKeyBuilder() {
            this.contextKey = new ContextKey();
        }

        public static ContextKeyBuilder get() {
            return new ContextKeyBuilder();
        }

        public ContextKeyBuilder withMajorKey( String majorKey ) {
            return addParam( majorKey, ( param ) -> this.contextKey.majorKey = param );
        }

        public ContextKeyBuilder withMinorKey( String minorKey ) {
            return addParam( minorKey, ( param ) -> this.contextKey.minorKey = param );
        }

        public ContextKeyBuilder withHolderClass( Class<?> holderClass ) {
            this.contextKey.holderClass = holderClass;

            return this;
        }

        public ContextKey build() throws InvalidBuildParamsException {
            this.assertParamsSetup();

            return this.contextKey;
        }

        private ContextKeyBuilder addParam(String param, Consumer<String> addParamAction) {
            addParamWithCheck( param, addParamAction );

            return this;
        }

        private void addParamWithCheck( String param, Consumer<String> addParamAction ) {
            if ( CheckingParamsUtil.isParamsNotNullOrEmpty( param ) ) {
                addParamAction.accept( param );
            }
        }

        private void assertParamsSetup() throws InvalidBuildParamsException {
            if ( CheckingParamsUtil.isParamsNullOrEmpty( this.contextKey.majorKey, this.contextKey.minorKey )
                    && Objects.isNull( this.contextKey )) {
                throw new InvalidBuildParamsException( "Invalid params" );
            }
        }
    }
}
