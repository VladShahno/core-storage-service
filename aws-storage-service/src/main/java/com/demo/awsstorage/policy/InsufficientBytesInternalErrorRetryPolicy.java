package com.demo.awsstorage.policy;

import com.demo.reststarter.exception.InternalErrorException;
import com.google.common.base.Strings;
import org.springframework.classify.Classifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;

public class InsufficientBytesInternalErrorRetryPolicy extends ExceptionClassifierRetryPolicy {

    private final int maxRetries;

    public InsufficientBytesInternalErrorRetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
        final RetryPolicy retryPolicy = defaultRetryPolicy();
        this.setExceptionClassifier((Classifier<Throwable, RetryPolicy>) classifiable -> {
            // Execute retry policy only if the error is due to insufficient bytes read by minio during
            // upload.
            if (classifiable instanceof InternalErrorException) {
                String statusMessage = ((InternalErrorException) classifiable).getMessage();
                if ((!Strings.isNullOrEmpty(statusMessage)) && (statusMessage.contains(
                    "binary.data.save.error"))) {
                    return retryPolicy;
                }
            }
            return neverRetry();
        });
    }

    private RetryPolicy neverRetry() {
        return new NeverRetryPolicy();
    }

    private RetryPolicy defaultRetryPolicy() {
        final SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy();
        simpleRetryPolicy.setMaxAttempts(maxRetries);
        return simpleRetryPolicy;
    }
}
