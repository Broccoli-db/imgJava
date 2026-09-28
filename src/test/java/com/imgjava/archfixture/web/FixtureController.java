package com.imgjava.archfixture.web;

import com.imgjava.archfixture.repository.FixtureRepository;

/**
 * 故意违规的 fixture：web 层直接依赖 repository 层，跳过了 service。
 *
 * <p><strong>仅供 ArchitectureTest 自检使用</strong>。如果分层规则是空跑的，
 * 自检用例会失败，从而暴露"规则没生效"这件事本身。
 */
public class FixtureController {

    private final FixtureRepository repository = new FixtureRepository();

    public String handle() {
        return repository.load();
    }
}
