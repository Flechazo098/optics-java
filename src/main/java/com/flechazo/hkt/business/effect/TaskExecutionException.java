package com.flechazo.hkt.business.effect;

import com.flechazo.hkt.business.capability.*;
import com.flechazo.hkt.business.control.*;
import com.flechazo.hkt.business.context.*;
import com.flechazo.hkt.business.core.*;
import com.flechazo.hkt.business.data.*;
import com.flechazo.hkt.business.effect.*;
import com.flechazo.hkt.business.stream.*;

public final class TaskExecutionException extends RuntimeException {
    public TaskExecutionException(Throwable cause) {
        super(cause);
    }
}
