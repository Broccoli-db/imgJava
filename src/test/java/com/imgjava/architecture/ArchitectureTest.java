package com.imgjava.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

/**
 * 分层架构约束，失败即构建失败。
 *
 * <p>每条规则同时作用于两个包根：
 * <ul>
 *   <li>{@code com.imgjava}（生产代码）——分层包内目前只有 {@code package-info.java}，
 *       还没有类，因此这些规则现在是<strong>棘轮</strong>：拦不下任何东西，但一旦有人在
 *       web 层直接调用 repository，构建就会红。这是有意为之。</li>
 *   <li>{@code com.imgjava.demo}（演示切片，位于 src/test）——有真实类，
 *       规则在这里被实际执行。</li>
 * </ul>
 *
 * <p>另有一条自检用例，用故意违规的 {@code com.imgjava.archfixture} 证明规则本身确实会拦，
 * 而不是因为匹配不到类而"通过"。
 */
class ArchitectureTest {

    private static final String PRODUCTION_ROOT = "com.imgjava";
    private static final String DEMO_ROOT = "com.imgjava.demo";
    private static final String FIXTURE_ROOT = "com.imgjava.archfixture";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(PRODUCTION_ROOT);

    private static final JavaClasses DEMO_CLASSES = new ClassFileImporter()
            .importPackages(DEMO_ROOT);

    // ---------------------------------------------------------------- 规则定义

    static ArchRule webMustNotDependOnRepository(String root) {
        return noClasses().that().resideInAPackage(root + ".web..")
                .should().dependOnClassesThat().resideInAPackage(root + ".repository..")
                .because("HTTP 层必须经过 service，不能直接访问数据访问层");
    }

    static ArchRule serviceMustNotDependOnWeb(String root) {
        return noClasses().that().resideInAPackage(root + ".service..")
                .should().dependOnClassesThat().resideInAPackage(root + ".web..")
                .because("依赖方向是 web -> service，不允许反向");
    }

    static ArchRule repositoryMustNotDependOnUpperLayers(String root) {
        return noClasses().that().resideInAPackage(root + ".repository..")
                .should().dependOnClassesThat().resideInAnyPackage(root + ".web..", root + ".service..")
                .because("数据访问层位于依赖方向下段，不得反向依赖");
    }

    static ArchRule domainMustNotDependOnOtherLayers(String root) {
        return noClasses().that().resideInAPackage(root + ".domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(root + ".web..", root + ".service..", root + ".repository..")
                .because("领域层是依赖方向的底端，只被依赖、不依赖上层");
    }

    static ArchRule webMustNotUsePersistenceApi(String root) {
        return noClasses().that().resideInAPackage(root + ".web..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                .because("持久化实体不应直接作为对外 DTO");
    }

    static ArchRule controllersMustBeNamedController(String root) {
        return classes().that().areAnnotatedWith(RestController.class)
                .and().resideInAPackage(root + ".web..")
                .should().haveSimpleNameEndingWith("Controller")
                .because("web 层的 REST 入口命名要可预期")
                // 生产包当前没有类，ArchUnit 默认会把"零匹配"判定为错误；
                // 这里允许空集，让它作为棘轮存在而不是误报。
                .allowEmptyShould(true);
    }

    static ArchRule repositoryInterfacesMustBeNamedRepository(String root) {
        return classes().that().resideInAPackage(root + ".repository..")
                .and().areInterfaces()
                // package-info.java 被 javac 编译成 ACC_INTERFACE|ACC_SYNTHETIC 的合成接口，
                // 会被 areInterfaces() 匹配到。它不是数据访问接口，必须排除，
                // 否则任何在 repository 包下放 package-info.java 的人都会撞上这条规则。
                .and().haveSimpleNameNotEndingWith("package-info")
                .should().haveSimpleNameEndingWith("Repository")
                .because("数据访问接口命名要可预期")
                .allowEmptyShould(true);
    }

    static ArchRule layersMustBeFreeOfCycles(String root) {
        return slices().matching(root + ".(*)..").should().beFreeOfCycles()
                .because("分层之间不允许循环依赖");
    }

    static ArchRule mustNotUseSystemOut(String root) {
        return noClasses().that().resideInAPackage(root + "..")
                .should().accessField(System.class, "out")
                .because("使用 SLF4J 记录日志，不用 System.out");
    }

    static ArchRule mustNotPrintStackTrace(String root) {
        return noClasses().that().resideInAPackage(root + "..")
                .should().callMethod(Throwable.class, "printStackTrace")
                .because("异常交给统一异常处理，不要直接打印堆栈");
    }

    static ArchRule mustNotUseJavaUtilLogging(String root) {
        return noClasses().that().resideInAPackage(root + "..")
                .should().dependOnClassesThat().resideInAPackage("java.util.logging..")
                .because("统一使用 SLF4J");
    }

    // ---------------------------------------------------------------- 生产 + 演示

    @Test
    @DisplayName("web 不得依赖 repository")
    void webMustNotDependOnRepositoryInBothScopes() {
        webMustNotDependOnRepository(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        webMustNotDependOnRepository(DEMO_ROOT).check(DEMO_CLASSES);
    }

    @Test
    @DisplayName("service 不得依赖 web")
    void serviceMustNotDependOnWebInBothScopes() {
        serviceMustNotDependOnWeb(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        serviceMustNotDependOnWeb(DEMO_ROOT).check(DEMO_CLASSES);
    }

    @Test
    @DisplayName("repository 不得依赖 web 或 service")
    void repositoryMustNotDependOnUpperLayersInBothScopes() {
        repositoryMustNotDependOnUpperLayers(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        repositoryMustNotDependOnUpperLayers(DEMO_ROOT).check(DEMO_CLASSES);
    }

    @Test
    @DisplayName("domain 不得依赖任何上层")
    void domainMustNotDependOnOtherLayersInBothScopes() {
        domainMustNotDependOnOtherLayers(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        domainMustNotDependOnOtherLayers(DEMO_ROOT).check(DEMO_CLASSES);
    }

    @Test
    @DisplayName("web 不得使用持久化 API")
    void webMustNotUsePersistenceApiInBothScopes() {
        webMustNotUsePersistenceApi(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        webMustNotUsePersistenceApi(DEMO_ROOT).check(DEMO_CLASSES);
    }

    @Test
    @DisplayName("命名约定符合分层")
    void namingConventionsHoldInBothScopes() {
        controllersMustBeNamedController(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        controllersMustBeNamedController(DEMO_ROOT).check(DEMO_CLASSES);
        repositoryInterfacesMustBeNamedRepository(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        repositoryInterfacesMustBeNamedRepository(DEMO_ROOT).check(DEMO_CLASSES);
    }

    @Test
    @DisplayName("分层之间没有循环依赖")
    void layersAreFreeOfCyclesInBothScopes() {
        layersMustBeFreeOfCycles(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        layersMustBeFreeOfCycles(DEMO_ROOT).check(DEMO_CLASSES);
    }

    @Test
    @DisplayName("不使用 System.out、printStackTrace、java.util.logging")
    void loggingDisciplineHoldsInBothScopes() {
        mustNotUseSystemOut(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        mustNotUseSystemOut(DEMO_ROOT).check(DEMO_CLASSES);
        mustNotPrintStackTrace(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        mustNotPrintStackTrace(DEMO_ROOT).check(DEMO_CLASSES);
        mustNotUseJavaUtilLogging(PRODUCTION_ROOT).check(PRODUCTION_CLASSES);
        mustNotUseJavaUtilLogging(DEMO_ROOT).check(DEMO_CLASSES);
    }

    // ---------------------------------------------------------------- 自检

    @Test
    @DisplayName("自检：故意违规的 fixture 必须被拦下，证明规则不是空跑")
    void rulesActuallyDetectViolations() {
        JavaClasses fixtureClasses = new ClassFileImporter().importPackages(FIXTURE_ROOT);

        assertThat(fixtureClasses.size())
                .as("fixture 必须被 ArchUnit 加载到，否则本用例本身就是空的")
                .isGreaterThan(0);

        assertThatThrownBy(() -> webMustNotDependOnRepository(FIXTURE_ROOT).check(fixtureClasses))
                .as("web 直接依赖 repository 必须被这条规则拦下")
                .isInstanceOf(AssertionError.class);
    }
}
