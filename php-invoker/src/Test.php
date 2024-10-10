<?php

namespace Invoker;

require '../vendor/autoload.php';

use Invoker\ParameterResolver\AssociativeArrayResolver;
use Invoker\ParameterResolver\DefaultValueResolver;
use Invoker\ParameterResolver\NumericArrayResolver;
use Invoker\ParameterResolver\ResolverChain;
use Invoker\ParameterResolver\TypeHintResolver;
use Invoker\Reflection\CallableReflection;

class Test
{
    public static function main(): void
    {
//
//        $callableReflection = new CallableReflection();
//        $r = $callableReflection->create([Test::class, "test01"]);
//
//
//        $resolverChain = new ResolverChain([
//            new NumericArrayResolver,
//            new AssociativeArrayResolver,
//            new DefaultValueResolver,
//            new TypeHintResolver,
//        ]);
//
//      // Invoker\Test
//
//        $parameters = $resolverChain->getParameters(
//            $r,
//            [Test::class => new Test()],
//            []
//        );
//
//
//        print_r($parameters);




    }


    public function test01(Test $test, string $name = "yangyi", int $age = 123)
    {

    }
}


Test::main();
