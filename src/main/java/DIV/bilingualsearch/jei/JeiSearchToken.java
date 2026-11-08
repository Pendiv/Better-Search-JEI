package DIV.bilingualsearch.jei;

import mezz.jei.gui.search.ElementPrefixParser.TokenInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public final class JeiSearchToken {
    private static final MethodHandle WITH_TEXT = findCopyHandle();

    private JeiSearchToken() {}

    private static MethodHandle findCopyHandle() {
        // PrefixInfo moved packages between JEI releases. Bind the getter and constructor together.
        try {
            var getter = TokenInfo.class.getMethod("prefixInfo");
            var constructor = TokenInfo.class.getConstructor(String.class, getter.getReturnType());
            var lookup = MethodHandles.publicLookup();
            return MethodHandles.collectArguments(lookup.unreflectConstructor(constructor), 1, lookup.unreflect(getter));
        } catch (ReflectiveOperationException e) {
            System.getLogger(JeiSearchToken.class.getName()).log(System.Logger.Level.WARNING,
                    "Could not initialize JEI search token compatibility", e);
            return null;
        }
    }

    public static TokenInfo withText(TokenInfo original, String text) {
        if (WITH_TEXT == null) return null;
        try {
            return (TokenInfo) WITH_TEXT.invokeExact(text, original);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("Could not copy a JEI search token", e);
        }
    }
}
