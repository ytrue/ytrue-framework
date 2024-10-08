<?php

namespace Ioc;


/**
 *
 */
class Dog
{

    private DogName $dogName;
    private int $age;

    private DogName $dogName1;
    private int $age1;

    private DogName $dogName2;
    private int $age2;

    public function setDogName1(DogName $dogName1): void
    {
        $this->dogName1 = $dogName1;
    }

    public function setAge1(int $age1): void
    {
        $this->age1 = $age1;
    }

    public function setDogName2(DogName $dogName2): void
    {
        $this->dogName2 = $dogName2;
    }

    public function setAge2(int $age2): void
    {
        $this->age2 = $age2;
    }




    /**
     * @param DogName $dogName
     * @param int $age
     */
    public function __construct(DogName $dogName, int $age)
    {
        $this->dogName = $dogName;
        $this->age = $age;
    }

    public function getDogName(): DogName
    {
        return $this->dogName;
    }

    public function setDogName(DogName $dogName): void
    {
        $this->dogName = $dogName;
    }

    public function getAge(): int
    {
        return $this->age;
    }

    public function setAge(int $age): void
    {
        $this->age = $age;
    }




}
