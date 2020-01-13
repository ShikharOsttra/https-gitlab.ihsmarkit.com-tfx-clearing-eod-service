package com.ihsmarkit.tfx.eod.statemachine;

public class JobFailedException extends RuntimeException {
    public JobFailedException(final String message) {
        super(message);
    }
}
