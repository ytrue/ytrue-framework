<?php

namespace Ioc\Definition\Resolver;

use Ioc\Definition\Definition;

class DecoratorResolver implements DefinitionResolver
{

    #[\Override] public function resolve(Definition $definition, array $parameters = []): mixed
    {
        // TODO: Implement resolve() method.
    }

    #[\Override] public function isResolvable(Definition $definition, array $parameters = []): bool
    {
        // TODO: Implement isResolvable() method.
    }
}
