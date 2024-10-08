<?php


namespace Ioc;
include "../vendor/autoload.php";

use Ioc\Definition\Exception\InvalidDefinition;
use Ioc\Definition\Helper\CreateDefinitionHelper;
use Ioc\Definition\ObjectDefinition;
use Ioc\Definition\Resolver\ObjectCreator;
use Ioc\Definition\Resolver\ResolverDispatcher;
use Ioc\Definition\Source\DefinitionArray;
use Ioc\Definition\Source\DefinitionNormalizer;
use Ioc\Definition\Source\ReflectionBasedAutowiring;
use Ioc\Proxy\ProxyFactory;

class Test
{
    /**
     * @throws \Exception
     */
    public function main(): void
    {
//        $reflectionBasedAutowiring = new ReflectionBasedAutowiring();
//
//        $definitionNormalizer = new DefinitionNormalizer($reflectionBasedAutowiring);

//        $objectDefinition = new ObjectDefinition("dog_*_*_*_test");
//
//        $test = ["user","age"];
//
//        $objectDefinition->replaceWildcards($test);
//
//        print_r($objectDefinition);

      //  $objectDefinition = new ObjectDefinition("dog", Dog::class);


//        $createDefinitionHelper = new CreateDefinitionHelper();
//        $createDefinitionHelper->constructor(new DogName("test dog"), 1);
//
//        $objectDefinition1 = $createDefinitionHelper->getDefinition(Dog::class);
//
//        print_r($objectDefinition1);

//        $reflectionBasedAutowiring = new ReflectionBasedAutowiring();
//        $definitionNormalizer = new DefinitionNormalizer($reflectionBasedAutowiring);
//
//
//        $dogName = new DogName("test ...");
//        $dog = new Dog($dogName,12);
//
//        $objectDefinition = new ObjectDefinition("dog",Dog::class);
//        $methodInjection = new ObjectDefinition\MethodInjection('__construct',["0"=>$dogName,"1"=>123]);
//        $objectDefinition->setConstructorInjection($methodInjection);
//
//        // $definition = $definitionNormalizer->normalizeRootDefinition($dog, "dog");
//        $definition = $definitionNormalizer->normalizeRootDefinition($objectDefinition, "dog");
//
//      //  print_r($definition);
//
//
//        $arr = [1];
//
//
//        if (isset($arr[0])){
//            echo 123;
//        }


     //   $definitionArray     = new DefinitionArray([],null);

       // $definitionArray->addDefinitions(['1'=>1,'2'=>2]);
//        $test = ["test"=>1,2,3,4];
//
//        print_r(array_keys($test));


        $dogName = new DogName("test ...");
        $dogName1 = new DogName("test1 ...");
        $dogName2 = new DogName("test2 ...");

        $createDefinitionHelper = new CreateDefinitionHelper(Dog::class);
        $createDefinitionHelper->constructor($dogName,123);

        $createDefinitionHelper->property("dogName1",$dogName1);
        $createDefinitionHelper->property("age1",111);


        $createDefinitionHelper->method("setDogName2",$dogName2);
        $createDefinitionHelper->method("setAge2",222);
        $objectDefinition = $createDefinitionHelper->getDefinition("dog");



        // 代理
        $proxyFactory = new ProxyFactory();
        $container = new Container();
        $resolverDispatcher = new ResolverDispatcher($container, $proxyFactory);

        $objectCreator = new ObjectCreator($resolverDispatcher,$proxyFactory);
        $object = $objectCreator->resolve($objectDefinition);


        print_r($object);

    }
}


$test = new Test();
$test->main();
