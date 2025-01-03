package plutoproject.framework.paper.dependency;

import plutoproject.framework.common.dependency.Dependencies;

public class DependenciesImpl implements Dependencies {
    public static DependenciesImpl INSTANCE = new DependenciesImpl();

    private DependenciesImpl() {
    }

    @Override
    public String getDependenciesFileName() {
        return "paper-dependencies.txt";
    }
}
