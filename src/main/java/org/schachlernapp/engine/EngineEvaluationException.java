package org.schachlernapp.engine;

/** Unchecked, damit {@link EngineEvaluator#evaluateAsync(String)} als reiner {@code Supplier} laufen kann. */
public class EngineEvaluationException extends RuntimeException {

    public EngineEvaluationException(String message, Throwable cause) {
        super(message, cause);
    }
}
