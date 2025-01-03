package plutoproject.framework.velocity.dependency;

import plutoproject.framework.common.dependency.Dependencies;

public class DependenciesImpl implements Dependencies {
    public static DependenciesImpl INSTANCE = new DependenciesImpl();

    private DependenciesImpl() {
    }

    @Override
    public String getDependenciesFileName() {
        return "velocity-dependencies.txt";
    }
}
