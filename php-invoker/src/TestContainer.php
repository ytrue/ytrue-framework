<?php

namespace Invoker;

use Override;
use Psr\Container\ContainerInterface;

class TestContainer implements ContainerInterface
{

    #[Override] public function get(string $id): mixed
    {
        return null;
    }

    #[Override] public function has(string $id): bool
    {
        return true;
    }
}
