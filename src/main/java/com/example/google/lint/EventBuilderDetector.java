package com.example.google.lint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.*;
import lombok.ast.*;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class EventBuilderDetector extends Detector implements Detector.JavaScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "PostEventBuilder",
            "Post EventBuilder instance to Otto Bus instead of EventMessage",

            "Calling `post()` on an `EventBuilder` instance is usually a mistake. You must call " +
                    "`build()` on the `Builder` and finally `post()` the resulting `EventMessage`.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    EventBuilderDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    public static final String APPLICABLE_METHOD_NAME = "post";

    /** Constructs a new {@link EventBuilderDetector} check */
    public EventBuilderDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }


    // ---- Implements JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(APPLICABLE_METHOD_NAME);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
                            @NonNull MethodInvocation node) {
        log("visitMethod() called with " + "context = [" + context + "], visitor = [" + visitor + "], node = [" + node + "]");
        log("astValue = " + node.astName().astValue());
        assert node.astName().astValue().equals(APPLICABLE_METHOD_NAME);

        StrictListAccessor<Expression, MethodInvocation> args = node.astArguments();
        log("args size = " + args.size());
        if (args.size() == 1) {
            Expression argument = args.last();

            log("argument = " + argument.toString());
            log("argument class = " + argument.getClass());

            boolean shouldReport = false;
            if (argument instanceof ConstructorInvocation) {
                ConstructorInvocation invocation = (ConstructorInvocation) argument;
                String constructorRawType = invocation.rawTypeReference().toString();
                if (constructorRawType.equals("EventBuilder")) {
                    shouldReport = true;
                }
            } else if (argument instanceof MethodInvocation) {
                MethodInvocation invocation = (MethodInvocation) argument;
                Node operand = invocation.rawOperand();
                Identifier methodIdentifier = invocation.astName();
                log("invocation = " + invocation);
                log("operand = " + invocation.rawOperand());
                log("name = " + methodIdentifier);
                if (operand.toString().contains("EventBuilder")) {
                    if (!methodIdentifier.toString().equals("build")) {
                        shouldReport = true;
                    }
                }
            } else if (argument instanceof VariableReference) {
                VariableReference reference = (VariableReference) argument;
                // TODO post(builder) variable reference passed
            }

            if (shouldReport) {
                context.report(ISSUE, node, context.getLocation(node),
                        "EventBuilder posted to bus: did you forget to call `build()` ?");
            }
        }
    }

    private void log(String s) {
        System.out.println(s);
    }
}
