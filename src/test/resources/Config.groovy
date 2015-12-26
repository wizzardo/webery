key = 'value'
a.b = 'c'

a {
    c = 1
}

environments {
    test {
        env = 'test'
    }
    dev {
        env = 'dev'
    }
    prod {
        env = 'prod'
    }
}

environments {
    test {
        environment = 'test'
    }
    development {
        environment = 'development'
    }
    production {
        environment = 'production'
    }
}