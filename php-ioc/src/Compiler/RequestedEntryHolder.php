<?php

namespace Ioc\Compiler;

use Ioc\Factory\RequestedEntry;

readonly class RequestedEntryHolder implements RequestedEntry
{


    public function __construct(
        private string $name
    )
    {

    }

    #[\Override] public function getName(): string
    {
        return $this->name;
    }
}
