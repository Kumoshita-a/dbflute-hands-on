package org.docksidestage.handson.dbflute.allcommon;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import org.dbflute.exception.ClassificationNotFoundException;
import org.dbflute.jdbc.Classification;
import org.dbflute.jdbc.ClassificationCodeType;
import org.dbflute.jdbc.ClassificationMeta;
import org.dbflute.jdbc.ClassificationUndefinedHandlingType;
import org.dbflute.optional.OptionalThing;

/**
 * The definition of classification.
 * @author DBFlute(AutoGenerator)
 */
public interface CDef extends Classification {

    /**
     * フラグを示す
     */
    public enum Flg implements CDef {
        /** はい: 有効を示す */
        True("1", "はい"),
        /** いいえ: 無効を示す */
        False("0", "いいえ");
        private static ZzzoneSlimmer<Flg> _slimmer = new ZzzoneSlimmer<>(Flg.class, values());
        private String _code; private String _alias;
        private Flg(String code, String alias) { _code = code; _alias = alias; }
        public String code() { return _code; } public String alias() { return _alias; }
        public Set<String> sisterSet() { return Collections.emptySet(); }
        public Map<String, Object> subItemMap() { return Collections.emptyMap(); }
        public ClassificationMeta meta() { return CDef.DefMeta.Flg; }
        public boolean inGroup(String groupName) { return false; }
        /**
         * Get the classification of the code. (CaseInsensitive)
         * @param code The value of code, which is case-insensitive. (NullAllowed: if null, returns empty)
         * @return The optional classification corresponding to the code. (NotNull, EmptyAllowed: if not found, returns empty)
         */
        public static OptionalThing<Flg> of(Object code) { return _slimmer.of(code); }
        /**
         * Find the classification by the name. (CaseInsensitive)
         * @param name The string of name, which is case-insensitive. (NotNull)
         * @return The optional classification corresponding to the name. (NotNull, EmptyAllowed: if not found, returns empty)
         */
        public static OptionalThing<Flg> byName(String name) { return _slimmer.byName(name); }
        /**
         * <span style="color: #AD4747; font-size: 120%">Old style so use of(code).</span>
         * @param code The value of code, which is case-insensitive. (NullAllowed: if null, returns null)
         * @return The instance of the corresponding classification to the code. (NullAllowed: if not found, returns null)
         */
        public static Flg codeOf(Object code) { return _slimmer.codeOf(code); }
        /**
         * <span style="color: #AD4747; font-size: 120%">Old style so use byName(name).</span>
         * @param name The string of name, which is case-sensitive. (NullAllowed: if null, returns null)
         * @return The instance of the corresponding classification to the name. (NullAllowed: if not found, returns null)
         * @deprecated use byName(name) instead.
         */
        @Deprecated
        public static Flg nameOf(String name) { return _slimmer.nameOf(name, nm -> valueOf(nm)); }
        /**
         * Get the list of all classification elements. (returns new copied list)
         * @return The snapshot list of all classification elements. (NotNull)
         */
        public static List<Flg> listAll() { return _slimmer.listAll(values()); }
        /**
         * Get the list of classification elements in the specified group. (returns new copied list)
         * @param groupName The string of group name, which is case-insensitive. (NotNull)
         * @return The snapshot list of classification elements in the group. (NotNull)
         * @throws ClassificationNotFoundException When the group is not found.
         */
        public static List<Flg> listByGroup(String groupName) {
            if (groupName == null) { throw new IllegalArgumentException("The argument 'groupName' should not be null."); }
            throw new ClassificationNotFoundException("Unknown classification group: Flg." + groupName);
        }
        /**
         * <span style="color: #AD4747; font-size: 120%">Old style so use e.g. Stream API with of().</span>
         * @param codeList The list of plain code, which is case-insensitive. (NotNull)
         * @return The snapshot list of classification elements in the code list. (NotNull, EmptyAllowed: when empty specified)
         * @deprecated use e.g. Stream API with of() instead.
         */
        @Deprecated
        public static List<Flg> listOf(Collection<String> codeList) { return _slimmer.listOf(codeList); }
        /**
         * <span style="color: #AD4747; font-size: 120%">Old style so use listByGroup(groupName).</span>
         * @param groupName The string of group name, which is case-sensitive. (NullAllowed: if null, returns empty list)
         * @return The snapshot list of classification elements in the group. (NotNull, EmptyAllowed: if the group is not found)
         * @deprecated use listByGroup(groupName) instead.
         */
        @Deprecated
        public static List<Flg> groupOf(String groupName) { return new ArrayList<>(); }
        @Override public String toString() { return code(); }
    }

    /**
     * 入会から退会までの会員のステータスを示す
     */
    public enum MemberStatus implements CDef {
        /** 正式会員: 正式な会員としてサイトサービスが利用可能 */
        正式会員("FML", "正式会員"),
        /** 退会会員: 退会が確定した会員でサイトサービスはダメ */
        退会会員("WDL", "退会会員"),
        /** 仮会員: 入会直後のステータスで一部のサイトサービスが利用可能 */
        仮会員("PRV", "仮会員");
        private static ZzzoneSlimmer<MemberStatus> _slimmer = new ZzzoneSlimmer<>(MemberStatus.class, values());
        private String _code; private String _alias;
        private MemberStatus(String code, String alias) { _code = code; _alias = alias; }
        public String code() { return _code; } public String alias() { return _alias; }
        public Set<String> sisterSet() { return Collections.emptySet(); }
        public Map<String, Object> subItemMap() { return Collections.emptyMap(); }
        public ClassificationMeta meta() { return CDef.DefMeta.MemberStatus; }
        public boolean inGroup(String groupName) { return false; }
        /**
         * Get the classification of the code. (CaseInsensitive)
         * @param code The value of code, which is case-insensitive. (NullAllowed: if null, returns empty)
         * @return The optional classification corresponding to the code. (NotNull, EmptyAllowed: if not found, returns empty)
         */
        public static OptionalThing<MemberStatus> of(Object code) { return _slimmer.of(code); }
        /**
         * Find the classification by the name. (CaseInsensitive)
         * @param name The string of name, which is case-insensitive. (NotNull)
         * @return The optional classification corresponding to the name. (NotNull, EmptyAllowed: if not found, returns empty)
         */
        public static OptionalThing<MemberStatus> byName(String name) { return _slimmer.byName(name); }
        /**
         * <span style="color: #AD4747; font-size: 120%">Old style so use of(code).</span>
         * @param code The value of code, which is case-insensitive. (NullAllowed: if null, returns null)
         * @return The instance of the corresponding classification to the code. (NullAllowed: if not found, returns null)
         */
        public static MemberStatus codeOf(Object code) { return _slimmer.codeOf(code); }
        /**
         * <span style="color: #AD4747; font-size: 120%">Old style so use byName(name).</span>
         * @param name The string of name, which is case-sensitive. (NullAllowed: if null, returns null)
         * @return The instance of the corresponding classification to the name. (NullAllowed: if not found, returns null)
         * @deprecated use byName(name) instead.
         */
        @Deprecated
        public static MemberStatus nameOf(String name) { return _slimmer.nameOf(name, nm -> valueOf(nm)); }
        /**
         * Get the list of all classification elements. (returns new copied list)
         * @return The snapshot list of all classification elements. (NotNull)
         */
        public static List<MemberStatus> listAll() { return _slimmer.listAll(values()); }
        /**
         * Get the list of classification elements in the specified group. (returns new copied list)
         * @param groupName The string of group name, which is case-insensitive. (NotNull)
         * @return The snapshot list of classification elements in the group. (NotNull)
         * @throws ClassificationNotFoundException When the group is not found.
         */
        public static List<MemberStatus> listByGroup(String groupName) {
            if (groupName == null) { throw new IllegalArgumentException("The argument 'groupName' should not be null."); }
            throw new ClassificationNotFoundException("Unknown classification group: MemberStatus." + groupName);
        }
        /**
         * <span style="color: #AD4747; font-size: 120%">Old style so use e.g. Stream API with of().</span>
         * @param codeList The list of plain code, which is case-insensitive. (NotNull)
         * @return The snapshot list of classification elements in the code list. (NotNull, EmptyAllowed: when empty specified)
         * @deprecated use e.g. Stream API with of() instead.
         */
        @Deprecated
        public static List<MemberStatus> listOf(Collection<String> codeList) { return _slimmer.listOf(codeList); }
        /**
         * <span style="color: #AD4747; font-size: 120%">Old style so use listByGroup(groupName).</span>
         * @param groupName The string of group name, which is case-sensitive. (NullAllowed: if null, returns empty list)
         * @return The snapshot list of classification elements in the group. (NotNull, EmptyAllowed: if the group is not found)
         * @deprecated use listByGroup(groupName) instead.
         */
        @Deprecated
        public static List<MemberStatus> groupOf(String groupName) { return new ArrayList<>(); }
        @Override public String toString() { return code(); }
    }

    public enum DefMeta implements ClassificationMeta {
        /** フラグを示す */
        Flg(cd -> CDef.Flg.of(cd), nm -> CDef.Flg.byName(nm)
        , () -> CDef.Flg.listAll(), gp -> CDef.Flg.listByGroup(gp)
        , ClassificationCodeType.Number, ClassificationUndefinedHandlingType.LOGGING),

        /** 入会から退会までの会員のステータスを示す */
        MemberStatus(cd -> CDef.MemberStatus.of(cd), nm -> CDef.MemberStatus.byName(nm)
        , () -> CDef.MemberStatus.listAll(), gp -> CDef.MemberStatus.listByGroup(gp)
        , ClassificationCodeType.String, ClassificationUndefinedHandlingType.LOGGING);

        private static final Map<String, DefMeta> _nameMetaMap = new HashMap<>();
        static {
            for (DefMeta value : values()) {
                _nameMetaMap.put(value.name().toLowerCase(), value);
            }
        }
        private final Function<Object, OptionalThing<? extends Classification>> _ofCall;
        private final Function<String, OptionalThing<? extends Classification>> _byNameCall;
        private final Supplier<List<? extends Classification>> _listAllCall;
        private final Function<String, List<? extends Classification>> _listByGroupCall;
        private final ClassificationCodeType _codeType;
        private final ClassificationUndefinedHandlingType _undefinedHandlingType;
        private DefMeta(Function<Object, OptionalThing<? extends Classification>> ofCall
                      , Function<String, OptionalThing<? extends Classification>> byNameCall
                      , Supplier<List<? extends Classification>> listAllCall
                      , Function<String, List<? extends Classification>> listByGroupCall
                      , ClassificationCodeType codeType
                      , ClassificationUndefinedHandlingType undefinedHandlingType
                ) {
            _ofCall = ofCall;
            _byNameCall = byNameCall;
            _listAllCall = listAllCall;
            _listByGroupCall = listByGroupCall;
            _codeType = codeType;
            _undefinedHandlingType = undefinedHandlingType;
        }
        public String classificationName() { return name(); } // same as definition name

        public OptionalThing<? extends Classification> of(Object code) { return _ofCall.apply(code); }
        public OptionalThing<? extends Classification> byName(String name) { return _byNameCall.apply(name); }

        public Classification codeOf(Object code) // null allowed, old style
        { return of(code).orElse(null); }
        public Classification nameOf(String name) { // null allowed, old style
            if (name == null) { return null; } // for compatible
            return byName(name).orElse(null); // case insensitive
        }

        public List<Classification> listAll()
        { return toClsList(_listAllCall.get()); }
        public List<Classification> listByGroup(String groupName) // exception if not found
        { return toClsList(_listByGroupCall.apply(groupName)); }

        @SuppressWarnings("unchecked")
        private List<Classification> toClsList(List<?> clsList) { return (List<Classification>)clsList; }

        public List<Classification> listOf(Collection<String> codeList) { // copied from slimmer, old style
            if (codeList == null) {
                throw new IllegalArgumentException("The argument 'codeList' should not be null.");
            }
            List<Classification> clsList = new ArrayList<>(codeList.size());
            for (String code : codeList) {
                clsList.add(of(code).get());
            }
            return clsList;
        }
        public List<Classification> groupOf(String groupName) { // empty if not found, old style
            try {
                return listByGroup(groupName); // case insensitive
            } catch (IllegalArgumentException | ClassificationNotFoundException e) {
                return new ArrayList<>(); // null or not found
            }
        }

        public ClassificationCodeType codeType() { return _codeType; }
        public ClassificationUndefinedHandlingType undefinedHandlingType() { return _undefinedHandlingType; }

        public static OptionalThing<CDef.DefMeta> find(String classificationName) { // instead of valueOf()
            if (classificationName == null) { throw new IllegalArgumentException("The argument 'classificationName' should not be null."); }
            return OptionalThing.ofNullable(_nameMetaMap.get(classificationName.toLowerCase()), () -> {
                throw new ClassificationNotFoundException("Unknown classification: " + classificationName);
            });
        }
        public static CDef.DefMeta meta(String classificationName) { // old style so use find(name)
            return find(classificationName).orElseTranslatingThrow(cause -> {
                return new IllegalStateException("Unknown classification: " + classificationName);
            });
        }
    }

    public static class ZzzoneSlimmer<CLS extends CDef> {

        public static Set<String> toSisterSet(String[] sisters) { // used by initializer so static
            return Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(sisters)));
        }

        private final Class<CLS> _clsType;
        private final Map<String, CLS> _codeClsMap = new HashMap<>();
        private final Map<String, CLS> _nameClsMap = new HashMap<>();

        public ZzzoneSlimmer(Class<CLS> clsType, CLS[] values) {
            _clsType = clsType;
            initMap(values);
        }

        private void initMap(CLS[] values) {
            for (CLS value : values) {
                _codeClsMap.put(value.code().toLowerCase(), value);
                for (String sister : value.sisterSet()) {
                    _codeClsMap.put(sister.toLowerCase(), value);
                }
                _nameClsMap.put(value.name().toLowerCase(), value);
            }
        }

        public OptionalThing<CLS> of(Object code) {
            if (code == null) {
                return OptionalThing.ofNullable(null, () -> {
                    throw new ClassificationNotFoundException("null code specified");
                });
            }
            if (_clsType.isAssignableFrom(code.getClass())) {
                @SuppressWarnings("unchecked")
                CLS cls = (CLS) code;
                return OptionalThing.of(cls);
            }
            if (code instanceof OptionalThing<?>) {
                return of(((OptionalThing<?>) code).orElse(null));
            }
            return OptionalThing.ofNullable(_codeClsMap.get(code.toString().toLowerCase()), () -> {
                throw new ClassificationNotFoundException("Unknown classification code: " + code);
            });
        }

        public OptionalThing<CLS> byName(String name) {
            if (name == null) {
                throw new IllegalArgumentException("The argument 'name' should not be null.");
            }
            return OptionalThing.ofNullable(_nameClsMap.get(name.toLowerCase()), () -> {
                throw new ClassificationNotFoundException("Unknown classification name: " + name);
            });
        }

        public CLS codeOf(Object code) {
            if (code == null) {
                return null;
            }
            if (_clsType.isAssignableFrom(code.getClass())) {
                @SuppressWarnings("unchecked")
                CLS cls = (CLS) code;
                return cls;
            }
            return _codeClsMap.get(code.toString().toLowerCase());
        }

        public CLS nameOf(String name, java.util.function.Function<String, CLS> valueOfCall) {
            if (name == null) {
                return null;
            }
            try {
                return valueOfCall.apply(name);
            } catch (RuntimeException ignored) { // not found
                return null;
            }
        }

        public List<CLS> listAll(CLS[] clss) {
            return new ArrayList<>(Arrays.asList(clss));
        }

        public List<CLS> listOf(Collection<String> codeList) {
            if (codeList == null) {
                throw new IllegalArgumentException("The argument 'codeList' should not be null.");
            }
            List<CLS> clsList = new ArrayList<>(codeList.size());
            for (String code : codeList) {
                clsList.add(of(code).get());
            }
            return clsList;
        }
    }
}
