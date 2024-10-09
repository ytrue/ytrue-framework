<?php

namespace Ioc\Definition\Source;

use Ioc\Definition\Definition;
use Ioc\Definition\ObjectDefinition;
use Override;

class AttributeBasedAutowiring  implements Autowiring,DefinitionSource
{

    #[Override] public function autowire(string $name, ?ObjectDefinition $definition = null): ObjectDefinition|null
    {
        // TODO: Implement autowire() method.
    }

    #[Override] public function getDefinition(string $name): Definition|null
    {
        // TODO: Implement getDefinition() method.
    }

    #[Override] public function getDefinitions(): array
    {
        // TODO: Implement getDefinitions() method.
    }
}
