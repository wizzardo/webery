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

item {
    key = 'value'
}


foo.v = 'bar'
environments {
    dev {
        foo.v = 'foobar'
        bar {
            v = "${foo.v}"
        }
    }
}

profiles {
    a {
        sub {
            key = 'a'
        }
        profiles.b.sub.key='b'
    }
}